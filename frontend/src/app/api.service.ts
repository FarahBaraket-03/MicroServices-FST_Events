import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap, timer, map } from 'rxjs';
import { Participant, Logistics, EventInput } from './models';
@Injectable({providedIn:'root'})
export class ApiService {
 private http=inject(HttpClient); private prefix='/api';
 createEvent(e:EventInput){return this.http.post(`${this.prefix}/events`,e);}
 updateEvent(e:EventInput){return this.http.put(`${this.prefix}/events/${e.idEvent}`,e);}
 getEvent(id:number){return this.http.get<EventInput>(`${this.prefix}/events/${id}`);}
 deleteEvent(id:number){return this.http.delete(`${this.prefix}/events/${id}`);}
 events(){return this.http.get<any[]>(`${this.prefix}/event-stats`);}
 participants(){return this.http.get<Participant[]>(`${this.prefix}/participants`);}
 addParticipant(p:Participant){return this.http.post<Participant>(`${this.prefix}/participants`,p);}
 updateParticipant(p:Participant){return this.http.put<Participant>(`${this.prefix}/participants/${p.idPart}`,p);}
 deleteParticipant(id:number){return this.http.delete<void>(`${this.prefix}/participants/${id}`);}
 logistics(){return this.http.get<Logistics[]>(`${this.prefix}/logistics/queries`);}
 addLogistics(l:Logistics){return this.http.post(`${this.prefix}/logistics/commands`,l);}
 updateLogistics(l:Logistics){return this.http.put(`${this.prefix}/logistics/commands/${l.idLog}`,l);}
 deleteLogistics(id:string){return this.http.delete(`${this.prefix}/logistics/commands/${id}`);}
 // A command acknowledgement does not imply its asynchronous projection is ready.
 waitForProjection(){return timer(600);}
}
