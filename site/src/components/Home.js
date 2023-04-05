import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import React from 'react'
import bannerImage from '../../static/banner.jpg'

const style = {
  list: {
    listStyle: 'none',
    padding: '0'
  }
}

const features = [
  {
    title: 'Seamless',
    link: 'docs/Examples/index.html#basic',
    description: (
        <>
          Generate optimized <a
            href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> <a
            href="docs/Quickstart/index.html#static-client">client</a> or <a
            href="docs/Quickstart/index.html#server">server</a> bindings from existing
            public API methods at compile time.
        </>
    ),
  },
  {
    title: 'Flexible',
    link: 'docs/Examples/index.html#dynamic-payload',
    description: (
        <>
          Customize <a
            href="docs/Examples/index.html#data-serialization">data serialization</a>, <a
            href="docs/Examples/index.html#client-function-names">remote API function names</a>, <a
            href="docs/Examples/index.html#client-exceptions">RPC protocol errors</a> and <a
            href="docs/Examples/index.html#http-authentication">authentication</a>.
        </>
    ),
  },
  {
    title: 'Modular',
    link: 'docs/Examples/index.html#integration',
    description: (
        <>
          Choose plugins for <a href="docs/Plugins/index.html#rpc-protocol">RPC protocol</a>, <a
            href="docs/Plugins/index.html#effect-system">effect handling</a>, <a
            href="docs/Plugins/index.html#message-transport">transport protocol</a> and <a
            href="docs/Plugins/index.html#message-codec">message format</a>.
        </>
    ),
  },
  {
    title: 'Permissive',
    link: 'docs/Examples/index.html#metadata',
    description: (
        <>
          Access transport protocol <a href="docs/Examples/index.html#http-request">request</a> and <a
            href="docs/Examples/index.html#http-response">response</a> metadata via optional abstractions.
        </>
    ),
  },
  {
    title: 'Discoverable',
    link: 'docs/Examples/index.html#api-schema',
    description: (
        <>
          Consume and provide <a href="https://spec.open-rpc.org">OpenRPC</a> 1.3+ or <a
            href="https://github.com/OAI/OpenAPI-Specification">OpenAPI</a> 3.1+ API schemas
          via generated discovery functions.
        </>
    ),
  },
  {
    title: 'Compatible',
    link: 'https://mvnrepository.com/artifact/org.automorph/automorph',
    description: (
        <>
          Supports <a href="https://dotty.epfl.ch/">Scala</a> 3.2+ or 2.13+ on <a
            href="https://openjdk.java.net/">JRE</a> 11+ and has no dependencies other than <a
            href="http://www.slf4j.org/">SLF4J</a> logging API.
        </>
    ),
  },
  {
    title: 'RPC protocols',
    link: 'docs/Plugins/index.html#rpc-protocol',
    description: (
        <>
          <ul style={style.list}>
            <li><a href="https://www.jsonrpc.org/specification">JSON-RPC</a></li>
            <li><a href="docs/Web-RPC">Web-RPC</a></li>
          </ul>
        </>
    ),
  },
  {
    title: 'Transport protocols',
    link: 'docs/Plugins/index.html#message-transport',
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples/index.html#http-authentication">HTTP</a></li>
            <li><a href="docs/Examples/index.html#websocket-transport">WebSocket</a></li>
            <li><a href="docs/Examples/index.html#amqp-transport">AMQP</a></li>
          </ul>
        </>
    ),
  },
  {
    title: 'Effect handling',
    link: 'docs/Plugins/index.html#effect-system',
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples/index.html#synchronous-call">Synchronous</a></li>
            <li><a href="docs/Examples/index.html#asynchronous-call">Asynchronous</a></li>
            <li><a href="docs/Examples/index.html#effect-system">Monadic</a></li>
          </ul>
        </>
    ),
  },
]

function BannerRow() {
  const config = useDocusaurusContext().siteConfig
  return (
      <div className="row">
        <div className={'col col--12'}>
          <img src={bannerImage} alt={config.title}/>
          <h2>This is a preview of an upcoming release. Please do not attempt to use it but feel free to review the documentation.</h2>
        </div>
      </div>
  )
}

function TaglineRow() {
  const config = useDocusaurusContext().siteConfig
  return (
      <div className="row">
        <div className={'col col--12'}>
          <div className="text--center padding-vert--sm">
            <p style={{
              fontSize: '2rem',
              color: 'var(--ifm-menu-color)',
            }}>{config.tagline}</p>
          </div>
        </div>
      </div>
  )
}

function FeatureCell({ title, link, description }) {
  return (
      <div className={'col col--4'}>
        <div className="text--center padding-horiz--sm padding-vert--sm">
          <h2><a href={link}>{title}</a></h2>
          <p>{description}</p>
        </div>
      </div>
  )
}

function FeaturesRow() {
  return (
      <div className="row">
        {features.map((props, index) => (
            <FeatureCell key={index} {...props} />
        ))}
      </div>
  )
}

function DocumentationRow() {
  return (
      <div className="row">
        <div className={'col col--12'}>
          <div className="text--center padding-bottom--xl">
            <a className="button" href="docs/Quickstart/index.html" style={{
              color: 'var(--sidebar-background-color)',
              backgroundColor: 'var(--ifm-link-color)',
              fontSize: '1.5rem',
            }}>
              Get Started
            </a>
          </div>
        </div>
      </div>
  )
}

export function Home() {
  const config = useDocusaurusContext().siteConfig
  return (
      <Layout title='Home' description={config.tagline}>
        <div style={{
          backgroundColor: 'var(--sidebar-background-color)',
        }}>
          <div className="container">
            <BannerRow/>
            <TaglineRow/>
            <FeaturesRow/>
            <DocumentationRow/>
          </div>
        </div>
      </Layout>
  )
}

