"""Verify current docs on a fresh, intentional disposable dev instance."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import time
from urllib.error import HTTPError
from urllib.request import Request, build_opener, ProxyHandler

FIELDS = {'reservationId','userId','screeningId','seats','status','createdAt','cancelledAt',
          'approvalRequestedAt','approvalDeadline','approvalPolicy','approvalRequiredSeatIds',
          'authorizedApproverId','decision','approverId','decisionAt','expiredAt','movieTitle','hallName','startsAt'}

def verify(base):
    client=build_opener(ProxyHandler({})); calls=[]
    def request(method,path,expected=200,body=None,actor=1):
        headers={'Accept':'application/json','X-User-Id':str(actor)}
        if body is not None:headers['Content-Type']='application/json'
        query=Request(base.rstrip('/')+path,method=method,headers=headers,
                      data=json.dumps(body).encode() if body is not None else None)
        try:response=client.open(query,timeout=15)
        except HTTPError as error:response=error
        with response:
            raw=response.read().decode(); data=json.loads(raw) if raw else None
            calls.append(dict(method=method,path=path,status=response.code,request=body,response=data))
            assert response.code==expected,calls[-1]
            return data
    def state(result,expected):
        assert set(result)==FIELDS and result['status']==expected,result
        return result
    def instant(value):return datetime.fromisoformat(value.replace('Z','+00:00'))
    screenings=request('GET','/screenings?upcoming=true'); assert screenings
    screening=screenings[0]; sid=screening['id']
    layout=request('GET',f'/screenings/{sid}/availability')
    assert all(set(s)=={'seatId','rowNumber','seatNumber','seatType','status'} for s in layout)
    standard=next(s['seatId'] for s in layout if s['seatType']=='STANDARD' and s['status']=='AVAILABLE')
    vip=next(s['seatId'] for s in layout if s['seatType']=='VIP' and s['status']=='AVAILABLE')
    def draft(seats):
        result=state(request('POST','/reservations',201,dict(userId=1,screeningId=sid,seatIds=seats)),'DRAFT')
        assert result['seats']==sorted(seats) and result['createdAt'] and result['approvalDeadline'] is None
        return result['reservationId']
    def change(id,operation,expected=200,body=None,actor=1):
        return request('POST',f'/reservations/{id}/{operation}',expected,body,actor)
    def availability(ids,expected):
        result=request('GET',f'/screenings/{sid}/availability')
        assert all(next(s for s in result if s['seatId']==id)['status']==expected for id in ids)
    direct=draft([standard]);availability([standard],'AVAILABLE')
    state(change(direct,'confirm'),'CONFIRMED');availability([standard],'UNAVAILABLE')
    state(change(direct,'cancel'),'CANCELLED')
    mixed=draft([standard,vip]);state(change(mixed,'confirm'),'PENDING_APPROVAL')
    availability([standard,vip],'UNAVAILABLE')
    change(mixed,'approval',403,{'decision':'APPROVE'});change(mixed,'approval',403,{'decision':'REJECT'})
    approved=state(change(mixed,'approval',body={'decision':'APPROVE'},actor=3),'CONFIRMED')
    assert approved['approverId']==3 and approved['decisionAt']
    availability([standard,vip],'UNAVAILABLE');state(change(mixed,'cancel'),'CANCELLED')
    rejected=draft([standard,vip]);state(change(rejected,'confirm'),'PENDING_APPROVAL')
    state(change(rejected,'approval',body={'decision':'REJECT'},actor=3),'REJECTED')
    availability([standard,vip],'AVAILABLE');change(rejected,'approval',409,{'decision':'APPROVE'},3)
    cancelled=draft([vip]);state(change(cancelled,'confirm'),'PENDING_APPROVAL');state(change(cancelled,'cancel'),'CANCELLED')
    expiring=draft([standard,vip]);pending=state(change(expiring,'confirm'),'PENDING_APPROVAL')
    competitor=draft([standard,vip]);change(competitor,'confirm',409)
    state(request('GET',f'/reservations/{competitor}'),'DRAFT')
    assert instant(pending['approvalDeadline'])<=instant(screening['startsAt'])
    while datetime.now(timezone.utc)<instant(pending['approvalDeadline']):time.sleep(.1)
    availability([standard,vip],'AVAILABLE')
    expired=state(request('GET',f'/reservations/{expiring}'),'EXPIRED')
    assert expired['expiredAt']==pending['approvalDeadline']
    change(expiring,'approval',409,{'decision':'APPROVE'},3);change(expiring,'cancel',409)
    state(change(competitor,'confirm'),'PENDING_APPROVAL');change(expiring,'approval',409,{'decision':'APPROVE'},3)
    state(change(competitor,'cancel'),'CANCELLED')
    movie=request('POST','/movies',201,{'title':'Runtime movie'});mid=movie['id']
    request('GET','/movies');request('GET',f'/movies/{mid}');request('PUT',f'/movies/{mid}',body={'title':'Edited movie'})
    hall=request('POST','/halls',201,{'name':'Runtime hall'});hid=hall['id']
    request('GET','/halls');request('GET',f'/halls/{hid}');request('PUT',f'/halls/{hid}',body={'name':'Edited hall'})
    seats=request('PUT',f'/halls/{hid}/layout',body={'rows':1,'seatsPerRow':2});request('GET',f'/halls/{hid}/seats')
    request('PUT',f'/halls/{hid}/seats/{seats[0]["id"]}',body={'seatType':'VIP'})
    body=dict(movieId=mid,hallId=hid,startsAt=screening['startsAt'])
    created=request('POST','/screenings',201,body);request('GET','/screenings')
    before=request('GET',f'/screenings/{created["id"]}');request('PUT',f'/screenings/{created["id"]}',405,body)
    assert request('GET',f'/screenings/{created["id"]}')==before
    assert set(before)=={'id','movie','hall','startsAt'}
    request('PUT',f'/halls/{hid}/layout',409,{'rows':2,'seatsPerRow':2});request('DELETE',f'/movies/{mid}',409)
    unused=request('POST','/movies',201,{'title':'Unused'});request('DELETE',f'/movies/{unused["id"]}',204)
    api=request('GET','/v3/api-docs')
    assert '/movies/{id}/deactivate' not in api['paths'] and '/screenings/{id}/cancel' not in api['paths']
    return dict(result='PASS',suite='current C08 contracts and A-F',timestamp=datetime.now(timezone.utc).isoformat(),
                base_url=base,screening_id=sid,standard_seat=standard,vip_seat=vip,
                observed_timeout_seconds=(instant(pending['approvalDeadline'])-instant(pending['approvalRequestedAt'])).total_seconds(),
                request_count=len(calls),calls=calls)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url',default='http://localhost:8080')
    parser.add_argument('--output',type=Path,default=Path(__file__).resolve().parents[1]/'docs/05_verification_and_quality/RUNTIME_VERIFICATION.json')
    args=parser.parse_args();report=verify(args.base_url)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='calls'},indent=2))
