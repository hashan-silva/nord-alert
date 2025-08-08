import { request } from 'undici';

export interface Region {
  code: string;
  name: string;
}

export interface RegionLists {
  counties: Region[];
  municipalities: Region[];
}

/**
 * Fetches county and municipality codes/names from the SCB PxWeb API.
 */
export async function fetchRegionLists(): Promise<RegionLists> {
  // Any PxWeb table exposing the Region variable works. This one contains both
  // county (two-digit) and municipality (four-digit) codes.
  const { body } = await request(
    'https://api.scb.se/OV0104/v1/AM/AM0101/Population/'
  );
  const meta = (await body.json()) as any;
  const regionVar = meta.variables.find((v: any) => v.code === 'Region');
  const codes: string[] = regionVar.values;
  const names: string[] = regionVar.valueTexts;
  const entries = codes.map((code, i) => ({ code, name: names[i] }));
  return {
    counties: entries.filter((e) => e.code.length === 2),
    municipalities: entries.filter((e) => e.code.length === 4),
  };
}
