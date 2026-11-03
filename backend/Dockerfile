FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY src ./src
COPY test ./test
EXPOSE 8080
CMD ["npm", "start"]
