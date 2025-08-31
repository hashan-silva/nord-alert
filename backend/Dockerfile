FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
COPY --from=build /app/dist ./dist
COPY package*.json ./
RUN npm ci --omit=dev \
  && chown -R node:node /app
USER node
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["node","-e","const u='http://127.0.0.1:'+(process.env.PORT||3000)+'/health'; const c=new AbortController(); const t=setTimeout(()=>c.abort(),3000); fetch(u,{signal:c.signal}).then(r=>{clearTimeout(t); process.exit(r.ok?0:1)}).catch(()=>{clearTimeout(t); process.exit(1)});"]
CMD ["node", "dist/index.js"]
