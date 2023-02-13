import clsx from 'clsx'
import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import Link from '@docusaurus/Link'
import React from 'react'
import homeImage from "../../../docs/images/home.jpg";

const style = {
  headerText: {
    position: 'absolute',
    top: '80%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    textAlign: 'center'
  },
  subtitle: {
    fontSize: '2rem',
    color: '#222222'
  },
  button: {
    margin: '0rem 1rem 3rem 1rem',
    color: '#000000',
    fontSize: '1.4rem'
  },
  features: {
    display: 'flex',
    alignItems: 'center',
    padding: '2rem 0'
  },
  list: {
    listStyle: 'none',
    padding: '0'
  }
}

const features = [
  {
    title: <a href="docs/Quickstart">Automatic</a>,
    description: (
        <>
          Generate <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0</a> and <a
            href="docs/Web-RPC/index.html">Web-RPC 0.1</a> client or server at compile-time from public API class methods.
        </>
    ),
  },
  {
    title: <a href="docs/Examples#select">Modular</a>,
    description: (
        <>
          Choose plugins for <a href="docs/Plugins#rpc-protocol">RPC</a> protocol, <a
            href="docs/Plugins#effect-system">effect type</a>, <a
            href="docs/Plugins#message-transport">transport protocol</a> and <a
            href="docs/Plugins#message-codec">message format</a>.
        </>
    ),
  },
  {
    title: <a href="https://mvnrepository.com/artifact/automorph">Compatible</a>,
    description: (
        <>
          Artifacts are available for <a href="https://dotty.epfl.ch/">Scala 3.2+</a> on <a
            href="https://openjdk.java.net/">JRE 11+</a> with support for <a
            href="https://www.scala-lang.org/news/2.13.0">Scala 2.13+</a> planned.
        </>
    ),
  },
  {
    title: <a href="docs/Examples#http-request-metadata">Escapable</a>,
    description: (
        <>
          Access transport protocol metadata (e.g. HTTP headers) using optional API abstractions.
        </>
    ),
  },
  {
    title: <a href="docs/Examples#api-schema-discovery">Discoverable</a>,
    description: (
        <>
          Consume and provide <a href="https://spec.open-rpc.org">OpenRPC 1.3+</a> or <a
            href="https://github.com/OAI/OpenAPI-Specification">OpenAPI 3.1+</a> API schemas via generated
          discovery functions.
        </>
    ),
  },
  {
    title: <a href="docs/Examples#data-serialization">Flexible</a>,
    description: (
        <>
          Customize data type serialization, remote API function names and RPC protocol errors.
        </>
    ),
  },
  {
    title: <a href="docs/Plugins#effect-system">Effect Types</a>,
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples#synchronous-call">Synchronous</a></li>
            <li><a href="docs/Examples#asynchronous-call">Asynchronous</a></li>
            <li><a href="docs/Examples#effect-system">Monadic</a></li>
          </ul>
        </>
    ),
  },
  {
    title: <a href="docs/Plugins#message-transport">Transport Protocols</a>,
    description: (
        <>
          <ul style={style.list}>
            <li><a href="docs/Examples#http-response-status">HTTP</a></li>
            <li><a href="https://en.wikipedia.org/wiki/WebSocket">WebSocket</a></li>
            <li><a href="https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol">AMQP</a></li>
          </ul>
        </>
    ),
  },
  {
    title: <a href="docs/Plugins#message-codec">Message Formats</a>,
    description: (
        <>
          <ul style={style.list}>
            <li><a href="https://www.json.org">JSON</a></li>
            <li><a href="https://msgpack.org">MessagePack</a></li>
          </ul>
        </>
    ),
  },
]

function Feature({ title, description }) {
  return (
      <div className={clsx('col col--4')}>
        <div className="text--center padding-horiz--md">
          <h3>{title}</h3>
          <p>{description}</p>
        </div>
      </div>
  )
}

export function Header() {
  const config = useDocusaurusContext().siteConfig
  return (
      <header>
        <div style={{
          backgroundColor: 'var(--sidebar-background-color)',
          position: 'relative'
        }}>
          <img src={homeImage} alt={config.title}/>
          <div style={style.headerText}>
            <p style={style.subtitle}>{config.tagline}</p>
            <div>
              <Link className="button button--lg" to="docs/Quickstart" style={style.button}>
                Get Started
              </Link>
            </div>
          </div>
        </div>
      </header>
  )
}

export function Features() {
  return (
      <section style={style.features}>
        <div className="container">
          <div className="row">
            {features.map((props, index) => (
                <Feature key={index} {...props} />
            ))}
          </div>
        </div>
      </section>
  )
}

export function Home() {
  const config = useDocusaurusContext().siteConfig
  return (
      <Layout title='Home' description={config.tagline}>
        <Header/>
        <main style={{
          backgroundColor: 'var(--sidebar-background-color)'
        }}>
          <Features/>
        </main>
      </Layout>
  )
}

