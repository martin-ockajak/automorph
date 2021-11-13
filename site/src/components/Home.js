import clsx from 'clsx'
import styles from './Home.module.css'
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
    title: 'Convenient',
    description: (
      <>
        Generate RPC client or server layer automatically at compile-time from public methods of API classes.
      </>
    ),
  },
  {
    title: 'Practical',
    description: (
      <>
        Access transport protocol request and response metadata using optional API abstractions.
      </>
    ),
  },
  {
    title: 'Flexible',
    description: (
      <>
        Customize remote API function names and mapping between exceptions and RPC protocol errors.
      </>
    ),
  },
  {
    title: 'Modular',
    description: (
      <>
        Choose plugins to select <a href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> protocol, <a href="https://en.wikipedia.org/wiki/Effect_system">effect</a> type, <a href="https://en.wikipedia.org/wiki/Transport_layer">transport</a> protocol and message <a href="https://en.wikipedia.org/wiki/File_format">format</a>.
      </>
    ),
  },
  {
    title: 'Discoverable',
    description: (
      <>
        Consume or provide API schemas through generated yet adjustable discovery functions.
      </>
    ),
  },
  {
    title: 'Extensible',
    description: (
      <>
        Easily implement custom data type serialization support or additional integration plugins.
      </>
    ),
  },
  {
    title: 'Manageable',
    description: (
      <>
        Leverage extensive error handling and structured logging via <a href="http://www.slf4j.org/">SLF4J</a> to diagnose issues.
      </>
    ),
  },
  {
    title: 'Compatible',
    description: (
      <>
        Artifacts are available for <a href="https://dotty.epfl.ch/">Scala 3</a> on <a href="https://openjdk.java.net/">JRE 11+</a> with support for <a href="https://www.scala-lang.org/news/2.13.0">Scala 2.13</a> and <a href="https://www.scala-lang.org/news/2.12.0">Scala 2.12</a> planned.
      </>
    ),
  },
  {
    title: 'RPC protocols',
    description: (
      <>
        <ul style={style.list}>
          <li><a href="https://www.jsonrpc.org/specification">JSON-RPC</a></li>
          <li><a href="docs/REST-RPC/index.html">REST-RPC</a></li>
        </ul>
      </>
    ),
  },
  {
    title: 'Transport protocols',
    description: (
      <>
        <ul style={style.list}>
          <li><a href="https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol">HTTP</a></li>
          <li><a href="https://en.wikipedia.org/wiki/WebSocket">WebSocket</a></li>
          <li><a href="https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol">AMQP</a></li>
        </ul>
      </>
    ),
  },
  {
    title: 'Message formats',
    description: (
      <>
        <ul style={style.list}>
          <li><a href="https://www.json.org">JSON</a></li>
          <li><a href="https://msgpack.org">MessagePack</a></li>
        </ul>
      </>
    ),
  },
  {
    title: 'API schemas',
    description: (
      <>
        <ul style={style.list}>
          <li><a href="https://spec.open-rpc.org">OpenRPC</a></li>
          <li><a href="https://github.com/OAI/OpenAPI-Specification">OpenAPI</a></li>
        </ul>
      </>
    ),
  },
]

function Feature({title, description}) {
  return (
    <div className={clsx('col col--3')}>
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
        <img src={homeImage} alt={config.title} />
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
      <Header />
      <main style={{
	backgroundColor: 'var(--sidebar-background-color)'
      }}>
        <Features />
      </main>
    </Layout>
  )
}

