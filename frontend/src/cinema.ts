import { useEffect, useState } from 'react'
export type Movie = { id:number; title:string; description:string; genre:string; durationMinutes:number; releaseYear:number; posterUrl:string; active:boolean }
export type Hall = { id:number; name:string; active:boolean }
export type Screening = { id:number; movie:Movie; hall:Hall; startsAt:string; endsAt:string; cancelled:boolean }
export type Seat = { id:number; rowNumber:number; seatNumber:number; seatType:'STANDARD'|'VIP'; active:boolean }
export async function api<T>(path:string, method='GET', body?:unknown):Promise<T>{const r=await fetch(path,{method,headers:{Accept:'application/json','Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});const data=await r.json().catch(()=>null);if(!r.ok)throw new Error(data?.detail||'API request failed.');return data as T}
export function useProgramme(){const [movies,setMovies]=useState<Movie[]>([]),[screenings,setScreenings]=useState<Screening[]>([]),[error,setError]=useState('');useEffect(()=>{let live=true;Promise.all([api<Movie[]>('/movies'),api<Screening[]>('/screenings?upcoming=true')]).then(([m,s])=>{if(live){setMovies(m.filter(x=>x.active));setScreenings(s)}}).catch(e=>{if(live)setError(String(e))});return()=>{live=false}},[]);return {movies,screenings,error}}
