import { Component, ViewChild  } from '@angular/core';
import { CurrentDataService } from './current-data-service';

declare var jQuery: any;

@Component({
    moduleId: module.id,
    selector: '[the-navbar]',
    templateUrl: './navigation-bar.component.html',
    styleUrls: ['style.css']
})
export class NavigationBarComponent {

  constructor(private currentDataService: CurrentDataService) { }


  onClick(event) {
        this.currentDataService.setData(jQuery('#the-flowchart').flowchart('getData'));
        this.currentDataService.processData();
  }
  
	DeleteOp(data : any){
        jQuery('#the-flowchart').flowchart('deleteSelected');
        this.currentDataService.clearData();
        this.currentDataService.setData(jQuery('#the-flowchart').flowchart('getData'));
	}
}
