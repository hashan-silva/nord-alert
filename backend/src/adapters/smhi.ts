// This file will contain the adapter for fetching and normalizing data from the SMHI warnings API.
//
// API Endpoint: To be researched. Expected to be a JSON feed derived from CAP.
//
// Normalized structure:
// {
//   source: 'smhi',
//   id: string,
//   eventType: string,
//   level: 'yellow' | 'orange' | 'red',
//   description: string,
//   areas: string[],
//   validFrom: Date,
//   validTo: Date,
//   url: string
// }
