/*
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// import "@vaadin/vaadin-charts/theme/vaadin-chart-default-theme";

const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `
<custom-style>
  <style>
html {
      --lumo-font-size: 1rem;
      --lumo-font-size-xxxl: 1.75rem;
      --lumo-font-size-xxl: 1.375rem;
      --lumo-font-size-xl: 1.125rem;
      --lumo-font-size-l: 1rem;
      --lumo-font-size-m: 0.875rem;
      --lumo-font-size-s: 0.8125rem;
      --lumo-font-size-xs: 0.75rem;
      --lumo-font-size-xxs: 0.6875rem;
      --lumo-line-height-m: 1.4;
      --lumo-line-height-s: 1.2;
      --lumo-line-height-xs: 1.1;
      --lumo-border-radius: calc(var(--lumo-size-m) / 2);
      --lumo-size-xl: 3rem;
      --lumo-size-l: 2.5rem;
      --lumo-size-m: 2rem;
      --lumo-size-s: 1.75rem;
      --lumo-size-xs: 1.5rem;
      --lumo-space-xl: 1.875rem;
      --lumo-space-l: 1.25rem;
      --lumo-space-m: 0.625rem;
      --lumo-space-s: 0.3125rem;
      --lumo-space-xs: 0.1875rem;
    }
  </style>
</custom-style>

<dom-module id="button-style" theme-for="vaadin-button">
  <template>
    <style>
      :host(:not([theme~="tertiary"])) {
        background-image: linear-gradient(var(--lumo-tint-5pct), var(--lumo-shade-5pct));
        box-shadow: inset 0 0 0 1px var(--lumo-contrast-20pct);
      }

      :host(:not([theme~="tertiary"]):not([theme~="primary"]):not([theme~="error"]):not([theme~="success"])) {
        color: var(--lumo-body-text-color);
      }

      :host([theme~="primary"]) {
        text-shadow: 0 -1px 0 var(--lumo-shade-20pct);
      }
    </style>
  </template>
</dom-module>


<custom-style>
  <style>
    html {
      overflow:hidden;
    }
    vaadin-app-layout vaadin-tab a:hover {
      text-decoration: none;
    }
  </style>
</custom-style>

<dom-module id="app-layout-theme" theme-for="vaadin-app-layout">
  <template>
    <style>
      [part="navbar"] {
        align-items: center;
        justify-content: center;
      }
    </style>
  </template>
</dom-module>

<!--<dom-module id="chart" theme-for="vaadin-chart">-->
<!--  <template>-->
<!--    <style include="vaadin-chart-default-theme">-->
<!--      :host {-->
<!--        &#45;&#45;vaadin-charts-color-0: var(&#45;&#45;lumo-primary-color);-->
<!--        &#45;&#45;vaadin-charts-color-1: var(&#45;&#45;lumo-error-color);-->
<!--        &#45;&#45;vaadin-charts-color-2: var(&#45;&#45;lumo-success-color);-->
<!--        &#45;&#45;vaadin-charts-color-3: var(&#45;&#45;lumo-contrast);-->
<!--      }-->
<!--      .highcharts-container {-->
<!--        font-family: var(&#45;&#45;lumo-font-family);-->
<!--      }-->
<!--      .highcharts-background {-->
<!--        fill: var(&#45;&#45;lumo-base-color);-->
<!--      }-->
<!--      .highcharts-title {-->
<!--        fill: var(&#45;&#45;lumo-header-text-color);-->
<!--        font-size: var(&#45;&#45;lumo-font-size-xl);-->
<!--        font-weight: 600;-->
<!--        line-height: var(&#45;&#45;lumo-line-height-xs);-->
<!--      }-->
<!--      .highcharts-legend-item text {-->
<!--        fill: var(&#45;&#45;lumo-body-text-color);-->
<!--      }-->
<!--      .highcharts-axis-title,-->
<!--      .highcharts-axis-labels {-->
<!--        fill: var(&#45;&#45;lumo-secondary-text-color);-->
<!--      }-->
<!--      .highcharts-axis-line,-->
<!--      .highcharts-grid-line,-->
<!--      .highcharts-tick {-->
<!--        stroke: var(&#45;&#45;lumo-contrast-10pct);-->
<!--      }-->
<!--      .highcharts-column-series rect.highcharts-point {-->
<!--        stroke: var(&#45;&#45;lumo-base-color);-->
<!--      }-->
<!--    </style>-->
<!--  </template>-->
<!--</dom-module>-->`;

document.head.appendChild($_documentContainer.content);
