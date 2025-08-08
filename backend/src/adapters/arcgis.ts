import { request } from 'undici';

/**
 * Fetch GeoJSON polygons for all municipalities.
 */
export async function fetchMunicipalityPolygons(): Promise<any> {
  const { body } = await request(
    'https://ext-geodata.lansstyrelsen.se/arcgis/rest/services/lsb/Kommuner/MapServer/1/query?where=1=1&outFields=KOMMUNNAMN,KOMMUNKOD,LANSKOD&outSR=4326&f=geojson'
  );
  return body.json();
}

/**
 * Fetch GeoJSON polygons for all counties.
 */
export async function fetchCountyPolygons(): Promise<any> {
  const { body } = await request(
    'https://ext-geodata.lansstyrelsen.se/arcgis/rest/services/lsb/Lan/MapServer/2/query?where=1=1&outFields=LANSNAMN,LAN_KOD,LANSKOD&outSR=4326&f=geojson'
  );
  return body.json();
}
