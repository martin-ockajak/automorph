import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import Link from '@docusaurus/Link'
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
    link: 'docs/Quickstart',
    description: (
        <>
          Generate <Link to="https://www.jsonrpc.org/specification">JSON-RPC</Link> 2.0 and <Link
            to="docs/Web-RPC/index.html">Web-RPC</Link> 0.1 client or server at compile-time from public API class
          methods.
        </>
    ),
  },
  {
    title: 'Modular',
    link: '"docs/Examples#select',
    description: (
        <>
          Choose plugins for <Link to="docs/Plugins#rpc-protocol">RPC</Link> protocol, <Link
            to="docs/Plugins#effect-system">effect type</Link>, <Link
            to="docs/Plugins#message-transport">transport protocol</Link> and <Link
            to="docs/Plugins#message-codec">message format</Link>.
        </>
    ),
  },
  {
    title: 'Flexible',
    link: 'docs/Examples#data-serialization',
    description: (
        <>
          Customize data type serialization, remote API function names and RPC protocol errors.
        </>
    ),
  },
  {
    title: 'Escapable',
    link: 'docs/Examples#http-request-metadata',
    description: (
        <>
          Access transport protocol metadata (e.g. HTTP headers) using optional API abstractions.
        </>
    ),
  },
  {
    title: 'Discoverable',
    link: 'docs/Examples#api-schema-discovery',
    description: (
        <>
          Consume and provide <a href="https://spec.open-rpc.org">OpenRPC</a> 1.3+ or <a
            href="https://github.com/OAI/OpenAPI-Specification">OpenAPI</a> 3.1+ API schemas via generated
          discovery functions.
        </>
    ),
  },
  {
    title: 'Compatible',
    link: 'https://mvnrepository.com/artifact/org.automorph/automorph',
    description: (
        <>
          Artifacts are available for <Link to="https://dotty.epfl.ch/">Scala</Link> 3.2+ on <Link
            to="https://openjdk.java.net/">JRE</Link> 11+ with support for <Link
            to="https://www.scala-lang.org/news/2.13.0">Scala</Link> 2.13+ planned.
        </>
    ),
  },
  {
    title: 'Effect Types',
    link: 'docs/Plugins#effect-system',
    description: (
        <>
          <ul style={style.list}>
            <li><Link to="docs/Examples#synchronous-call">Synchronous</Link></li>
            <li><Link to="docs/Examples#asynchronous-call">Asynchronous</Link></li>
            <li><Link to="docs/Examples#effect-system">Monadic</Link></li>
          </ul>
        </>
    ),
  },
  {
    title: 'Transport Protocols',
    link: 'docs/Plugins#message-transport',
    description: (
        <>
          <ul style={style.list}>
            <li><Link to="docs/Examples#http-response-status">HTTP</Link></li>
            <li><Link to="docs/Examples#websocket-transport">WebSocket</Link></li>
            <li><Link to="docs/Examples#amqp-transport">AMQP</Link></li>
          </ul>
        </>
    ),
  },
  {
    title: 'Message Formats',
    link: 'docs/Plugins#message-codec',
    description: (
        <>
          <ul style={style.list}>
            <li><Link to="docs/Examples#data-serialization">JSON</Link></li>
            <li><Link to="docs/Examples#message-codec">MessagePack</Link></li>
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
              color: 'var(--placeholder-color)',
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
          <h2><Link to={link}>{title}</Link></h2>
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
          <div className="text--center padding-bottom--lg">
            <Link className="button" to="docs/Quickstart" style={{
              color: 'var(--ifm-background-color)',
              backgroundColor: 'var(--ifm-link-color)',
              fontSize: '2rem',
            }}>
              Get Started
            </Link>
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

