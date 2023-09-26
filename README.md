
# LookupLab-UI

LookupLab-UI is the frontend complement to [LookupLab](https://github.com/MagentaHealth/LookupLab/tree/master).

This tool has been tailored to Magenta Health's needs, but some basic configuration options have been provided for organizations that wish to leverage what we have built.


## Requirements

- [node.js](https://nodejs.org) 6+
- [npm](https://www.npmjs.com) (comes bundled with `node.js`)
- [Java SDK](https://www.azul.com/downloads/) 11+


### Development
```
npm install
npx shadow-cljs watch app
```

Watch and compile sass
```
npx sass --watch public/sass/site.scss:public/css/site.css
```

Start a ClojureScript REPL
```
npx shadow-cljs browser-repl
```


### Configuration

`public/index.html`(/public/index.html) contains some basic configuration options.

- `apiURL` - the base URL of your [LookupLab](https://github.com/MagentaHealth/LookupLab/tree/master) deployment
- `siteURL` - your website's URL
- `audienceConfig` - controls what groups of triggers are available (`"all"`, `"patients"`, or `"others"`)
- `orgName` - your oranization's name
- `homePage` - your organization's homepage
- `bookingPage` - your organization's appointment booking page
- `resourcesPage` - your organization's patient resources page
- `registrationPage` - your organization's patient registration page
- `thirdPartyPage` - your organization's page with information for third parties
- `prompts` - an array of prompts that will display above the search bar
- `placeholders` - an array of example searches that will display in the search bar


### Packaging / Deployment

```
npx shadow-cljs release app
npx sass public/sass/site.scss:public/css/site.css
```

This will create two files - `public/js/app.js` and `public/css/site.css`.

The app can be published anywhere - as its own standalone page or within another existing page - so long as the necessary resources, configuration, and HTML elements are included. See `public/index.html`(/public/index.html) for an example setup.


#### Resources

- [Bulma](https://bulma.io/) - CSS framework
- [Ficons](https://ficons.fiction.com/) - icons

#### Configuration 

See above

#### HTML elements

The following HTML needs to be included in your webpage in order for the app to render correctly.

```html
<div id="app">
  <div class="app-loader-container">
    <span class="app-loader"></span>
  </div>
</div>
```


## License 

Copyright &copy; 2022-2023 [Magenta Health Inc](https://www.magentahealth.ca/).<br>
Authored by [Carmen La](https://carmen.la/).

This software is distributed under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
