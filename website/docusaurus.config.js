// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const config = {
  title: 'Automorph',
  tagline: 'RPC client and server library for Scala',
  url: 'https://automorph.org',
  baseUrl: '/',
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
          path: process.env['DOCS_PATH'] ?? 'docs',
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
//    'docusaurus-plugin-relative-paths'
  ],

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

