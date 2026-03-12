# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json ./
COPY package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
ARG REACT_APP_BACKEND_BASE_URL=http://localhost:8080
ARG REACT_APP_BACKEND_VERSION=unknown
ENV REACT_APP_BACKEND_BASE_URL=${REACT_APP_BACKEND_BASE_URL}
ENV REACT_APP_BACKEND_VERSION=${REACT_APP_BACKEND_VERSION}
RUN npm run build

FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf \
  && chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /var/run /etc/nginx/conf.d
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
USER 101:101
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
