// This file will contain the adapter for fetching and normalizing data from the Krisinformation v3 API.
//
// API Host: api.krisinformation.se
// Endpoints: /v3/news, /v3/vmas
//
// Normalized structure:
// {
//   source: 'krisinfo',
//   id: string,
//   headline: string,
//   preamble: string,
//   counties: string[],
//   publishedAt: Date,
//   url: string,
//   pushMessage?: string
// }
