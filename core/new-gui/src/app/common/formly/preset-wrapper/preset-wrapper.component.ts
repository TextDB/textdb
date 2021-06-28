import { Component, OnDestroy, OnInit } from '@angular/core';
import { FieldWrapper, FormlyFieldConfig } from '@ngx-formly/core';
import { merge } from 'lodash';
import { ReplaySubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Preset, PresetService } from 'src/app/workspace/service/preset/preset.service';
import { asType } from '../../util/assert';

/**
 * PresetWrapperComponent is a custom formly form field wrapper: https://formly.dev/guide/custom-formly-wrapper
 * It uses PresetService to create a dropdown menu for a form field that includes preset entries that when clicked are 
 * applied through the PresetService (generating an event in PresetService.applyPresetStream).
 * Currently the PresetService only truly handles operator presets. (i.e. causing the for data to change immediately)
 * For non-operator presets, an application event is generated, but no action is taken (please implement listeners to apply the presets properly)  
 * USAGE:  
 * Formly field key should match attributes of preset  
 * FormlyFieldConfig.wrappers should include 'preset-wrapper'  
 * FormlyFieldConfig.templateOptions.presetKey should be a PresetKey 
 * @author Albert Liu
 */

/**
 * A PresetKey must be passed to PresetWrapperComponent via templateOptions.presetKey (add a template option to the formly field config)
 * This can be done easily by using PresetWrapperComponent.setupFieldConfig()
 */
export interface PresetKey {
  presetType: string;
  saveTarget: string;
  applyTarget: string;
}

@Component({
templateUrl: './preset-wrapper.component.html',
styleUrls: ['./preset-wrapper.component.scss'],
})
export class PresetWrapperComponent extends FieldWrapper implements OnInit, OnDestroy {

  public searchResults: Preset[] = []; // the list of presets shown in the dropdown
  private searchTerm: string = ''; // a copy of the form field value, used as a search term to narrow suggested presets
  private presetType: string = ''; // corresponds to type used in presetService.getPresets(type, target). Usually "operator"
  private saveTarget: string = ''; // corresponds to target used in presetService.getPresets(type, target). Usually operator type, i.e. "MySQLSource"
  private applyTarget: string = ''; // corresponds to target used in presetService.applyPreset(type, target). Usually operatorID, i.e "MySQLSource-operator-8fb88f81-1bb1-4b00-bbd1-3d2f23c5e1d7"
  private teardownObservable: ReplaySubject<boolean> = new ReplaySubject(1); // observable used OnDestroy to tear down subscriptions that takeUntil(teardownObservable)

  constructor( private presetService: PresetService ) { super(); }

  ngOnInit(): void {
    if (
      this.field.key === undefined ||
      this.field.templateOptions === undefined ||
      this.field.templateOptions.presetKey === undefined) {

      throw Error(`form preset-wrapper field ${this.field} doesn't contain necessary .key and .templateOptions.presetKey attributes`);
    }
    const presetKey = <PresetKey> this.field.templateOptions.presetKey;
    this.searchTerm = this.formControl.value !== null ? this.formControl.value : '';
    this.presetType = presetKey.presetType;
    this.saveTarget =  presetKey.saveTarget;
    this.applyTarget = presetKey.applyTarget;
    this.searchResults = this.getSearchResults(this.presetService.getPresets(this.presetType, this.saveTarget), this.searchTerm, true);

    this.handleSavePresets(); // handles when presets for this saveTarget change
    this.handleFieldValueChanges(); // handles updating search results as the user types
  }

  /**
   * applies preset using PresetService.savePresetsStream event/observable system
   * @param preset 
   */
  public applyPreset(preset: Preset) {
    this.presetService.applyPreset(this.presetType, this.applyTarget, preset);
  }

  public deletePreset(preset: Preset) {
    this.presetService.deletePreset(this.presetType, this.saveTarget, preset, `Deleted preset: ${this.getEntryTitle(preset)}`, 'error');
  }

  /**
   * Generates title for dropdown menu entries
   * @param preset to generate title for
   * @returns title
   */
  public getEntryTitle(preset: Preset): string {
    return preset[asType(this.field.key, 'string')].toString();
  }

  /**
   * Generates description of dropdown menu entries
   * @param preset to generate description of
   * @returns description
   */
  public getEntryDescription(preset: Preset): string {
    return Object.keys(preset).filter(key => key !== asType(this.field.key, 'string')).map(key => preset[key]).join(', ');
  }

  /**
   * Filters a Preset[], allowing only getEntryTitle(Preset) that start with searchTerm
   * @param presets Preset[]
   * @param searchTerm string
   * @param showAllResults whether or not to filter presets or allow all presets
   * @returns 
   */
  public getSearchResults(presets: Readonly<Preset[]>, searchTerm: string, showAllResults: boolean): Preset[] {
    if (showAllResults) {
      return presets.slice();
    } else {
      return presets.filter(
        preset => this.getEntryTitle(preset)
          .replace(/^\s+|\s+$/g, '').toLowerCase().startsWith(searchTerm.toLowerCase())
      );
    }
  }

  /**
   * updates search results when dropdown is activated (clicking form field opens dropdown)
   * @param visible Event value, whether or not dropdown is visible
   */
  public onDropdownVisibilityEvent(visible: boolean) {
    if (visible) {
      this.searchResults = this.getSearchResults(this.presetService.getPresets(this.presetType, this.saveTarget), this.searchTerm, true);
    }
  }

  /**
   * called when service is destroyed by angular.
   * tears down subscriptions that takeUntil(teardownObservable)
   */
   public ngOnDestroy() {
    this.teardownObservable.next(true);
    this.teardownObservable.complete();
  }

  /**
   * handles when presets for the current presetType are changed due to saving new presets
   * updates search results to account for new presets
   */
  private handleSavePresets() {
    this.presetService.savePresetsStream.pipe(takeUntil(this.teardownObservable))
      .filter((presets) => presets.type === this.presetType && presets.target === this.saveTarget)
      .subscribe({
        next: (saveEvent) => {
          this.searchResults = this.getSearchResults(saveEvent.presets, this.searchTerm, false);
        }
      });
  }

  /**
   * handles user typing into form field
   * updates earch results to account for filtering
   */
  private handleFieldValueChanges() {
    // WIERD CODE EXPLANATION: debounceTime(0)?
    // After a preset is applied (by clicking a dropdown entry), it changes a field value and 
    // activates this handler function.
    // updating the searchResults (which also changes the HTML template due to binding) too quickly
    // can sometimes interrupt the dropdown closing animation. (dropdown should close after clicking dropdown entry, but instead stays open)
    // hence the debounceTime(0) to slow this function down.
    this.formControl.valueChanges.debounceTime(0).pipe(takeUntil(this.teardownObservable))
      .subscribe({
        next: (value: string | number | boolean ) => {
          this.searchTerm = value.toString();
          this.searchResults = this.getSearchResults(this.presetService.getPresets(this.presetType, this.saveTarget), this.searchTerm, false);
        }
      });
  }

  /**
   * setup FormlyFieldConfig to use PresetWrapperComponent:
   * adds preset-wrapper and form-field (default wrapper) as wrappers
   * @param config FormlyFieldConfig to setup
   * @param presetType corresponds to type used in presetService.getPresets(type, target). Usually "operator"
   * @param saveTarget corresponds to target used in presetService.getPresets(type, target). Usually operator type, i.e. "MySQLSource"
   * @param applyTarget corresponds to target used in presetService.applyPreset(type, target). Usually operatorID, i.e "MySQLSource-operator-8fb88f81-1bb1-4b00-bbd1-3d2f23c5e1d7"
   */
  public static setupFieldConfig( config: FormlyFieldConfig, presetType: string, saveTarget: string, applyTarget: string) {
    const fieldConfig: FormlyFieldConfig = {
      wrappers: ['form-field', 'preset-wrapper'], // wrap form field in default theme and then preset wrapper
      templateOptions: {
        presetKey: <PresetKey> {
          presetType: presetType,
          saveTarget: saveTarget,
          applyTarget: applyTarget
        }
      }
    };
    merge(config, fieldConfig);
  }

}
