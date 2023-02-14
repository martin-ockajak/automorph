import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import React from 'react'
import bannerImage from '../../../docs/images/banner.jpg'

const style = {
  list: {
    listStyle: 'none',
    padding: '0'
  }
}

const features = [
  {
    title: 'Automatic',
    link: 'docs/Quickstart/index.html',
    description: (
        <>
          Generate <a href="https://www.jsonrpc.org/specification">JSON-RPC</a> 2.0 and <a
            href="docs/Web-RPC">Web-RPC</a> 0.1 client or server at compile-time from public API class methods.
        </>
    ),
  },
  {
    title: 'Modular',
    link: 'docs/Examples/index.html#select',
    description: (
        <>
          Choose plugins for <a href="docs/Plugins/index.html#rpc-protocol">RPC</a> protocol, <a
            href="docs/Plugins/index.html#effect-system">effect type</a>, <a
            href="docs/Plugins/index.html#message-transport">transport protocol</a> and <a
            href="docs/Plugins/index.html#message-codec">message format</a>.
        </>
    ),
  },
  {
    title: 'Flexible',
    link: 'docs/Examples/index.html#data-serialization',
    description: (
        <>
          Customize data type serialization, remote API function names and RPC protocol errors.
        </>
    ),
  },
  {
    title: 'Escapable',
    link: 'docs/Examples/index.html#http-request-metadata',
    description: (
        <>
          Access transport protocol metadata (e.g. HTTP headers) using optional API abstractions.
        </>
    ),
  },
  {
    title: 'Discoverable',
    link: 'docs/Examples/index.html#api-schema-discovery',
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
          Artifacts are available for <a href="https://dotty.epfl.ch/">Scala</a> 3.2+ on <a
            href="https://openjdk.java.net/">JRE</a> 11+ with support for <a
            href="https://www.scala-lang.org/news/2.13.10">Scala</a> 2.13+ planned.
        </>
    ),
  },
  {
    title: 'Effect Types',
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
  {
    title: 'Transport Protocols',
    link: 'docs/Plugins/index.html#message-transport',
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples/index.html#http-response-status">HTTP</a></li>
            <li><a href="docs/Examples/index.html#websocket-transport">WebSocket</a></li>
            <li><a href="docs/Examples/index.html#amqp-transport">AMQP</a></li>
          </ul>
        </>
    ),
  },
  {
    title: 'Message Formats',
    link: 'docs/Plugins/index.html#message-codec',
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples/index.html#data-serialization">JSON</a></li>
            <li><a href="docs/Examples/index.html#message-codec">MessagePack</a></li>
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

