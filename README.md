
### Development mode
```
npm install
npx shadow-cljs watch app
```
watch and compile sass
```
npx sass --watch public/sass/site.scss:public/css/site.css
```
start a ClojureScript REPL
```
npx shadow-cljs browser-repl
```
### Building for production

```
npx shadow-cljs release app
npx sass public/sass/site.scss:public/css/site.css
```
