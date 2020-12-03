import { Injectable } from '@angular/core';
import {
  TexeraWebsocketRequest, TexeraWebsocketEvent, TexeraWebsocketRequestTypeMap, TexeraWebsocketRequestTypes
} from '../../types/workflow-websocket.interface';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WorkflowWebsocketService {

  private static readonly TEXERA_WEBSOCKET_ENDPOINT = 'wsapi/workflow-websocket';

  private readonly websocket: WebSocketSubject<TexeraWebsocketEvent | TexeraWebsocketRequest>;
  private readonly webSocketObservable: Observable<TexeraWebsocketEvent>;

  constructor() {
    this.websocket = webSocket<TexeraWebsocketEvent | TexeraWebsocketRequest>(WorkflowWebsocketService.getWorkflowWebsocketUrl());
    this.webSocketObservable = this.websocket.share() as Observable<TexeraWebsocketEvent>;
    this.webSocketObservable.subscribe(data => {
      if (data.type === 'HelloWorldResponse') {
        console.log('hello world works: ' + data.message);
      }
    });
    this.send('HelloWorldRequest', {message: 'Texera on Amber'});
  }

  public websocketEvent(): Observable<TexeraWebsocketEvent> {
    return this.webSocketObservable;
  }

  public send<T extends TexeraWebsocketRequestTypes>(type: T, payload: TexeraWebsocketRequestTypeMap[T]): void {
    const request = {
      type,
      ...payload
    } as any as TexeraWebsocketRequest;
    this.websocket.next(request);
  }

  public static getWorkflowWebsocketUrl(): string {
    const websocketUrl = new URL(WorkflowWebsocketService.TEXERA_WEBSOCKET_ENDPOINT, window.location.origin);
    // replace protocol, so that http -> ws, https -> wss
    websocketUrl.protocol = websocketUrl.protocol.replace('http', 'ws');
    return websocketUrl.toString();
  }

}
