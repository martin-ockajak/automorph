// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const config = {
  title: 'Automorph',
  tagline: 'RPC client and server for Scala',
  url: 'https://automorph.org',
  baseUrl: '/',
  favicon: 'icon.jpg',
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
        googleAnalytics: false,
        gtag: false,
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
    [require.resolve('@easyops-cn/docusaurus-search-local'), {
      highlightSearchTermsOnTargetPage: true
    }],
    '@someok/docusaurus-plugin-relative-paths'
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      algolia: false,
      navbar: {
        title: '',
        logo: {
          alt: 'Automorph',
          src: 'icon.jpg',
        },
        items: [
          {
            href: '/docs/Quickstart',
            label: 'Documentation',
            position: 'left',
          },
          {
            href: '/api/index.html',
            label: 'API',
            position: 'left',
          },
          {
            href: 'https://github.com/martin-ockajak/automorph',
            label: 'Source',
            position: 'left',
          },
          {
            href: 'https://mvnrepository.com/artifact/org.automorph/automorph',
            label: 'Artifacts',
            position: 'left',
          },
        ],
      },
      prism: {
        theme: require('prism-react-renderer/themes/nightOwl'),
        darkTheme: require('prism-react-renderer/themes/nightOwl'),
        additionalLanguages: ['java', 'scala']
      },
    }),
};

module.exports = config;

