// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const config = {
  title: 'Automorph',
  tagline: 'RPC client and server for Scala',
  url: 'https://automorph.org',
  baseUrl: '/',
  favicon: 'icon.png',
  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',
  organizationName: 'automorph',
  projectName: 'automorph',

  presets: [
    [
      '@docusaurus/preset-classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        blog: false,
        sitemap: false,
        docs: {
          path: process.env['SITE_DOCS'] ?? '../docs',
          sidebarPath: require.resolve('./sidebars.js'),
        },
        theme: {
          customCss: require.resolve('./src/pages/custom.css'),
        },
      }),
    ],
  ],

  plugins: [
    require.resolve('docusaurus-lunr-search', {
      indexBaseUrl: true
    }),
  ].concat(process.env['SITE_LOCAL'] ? ['docusaurus-plugin-relative-paths'] : []),

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      algolia: false,
      googleAnalytics: false,
      gtag: false,
      navbar: {
        title: 'Automorph',
        logo: {
          alt: 'Automorph',
          src: 'icon.png',
        },
        items: [
          {
            type: 'doc',
            docId: 'Overview',
            position: 'left'
          },
          {
            href: '/api/index.html',
            label: 'API',
            position: 'left',
          },
          {
            href: 'https://mvnrepository.com/artifact/org.automorph/automorph',
            label: 'Artifacts',
            position: 'right',
          },
          {
            href: 'https://github.com/martin-ockajak/automorph',
            label: 'Source',
            position: 'right',
          },
        ],
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['java', 'scala']
      },
    }),
};

module.exports = config;

