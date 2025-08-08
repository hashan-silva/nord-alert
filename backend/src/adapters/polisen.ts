// This file will contain the adapter for fetching and normalizing data from the Polisen events API.
//
// API Endpoint: https://polisen.se/api/events
//
// Normalized structure:
// {
//   source: 'polisen',
//   id: string,
//   title: string,
//   type: string,
//   summary: string,
//   url: string,
//   occurredAt: Date,
//   location: {
//     name: string,
//     lat: number,
//     lon: number
//   }
// }
