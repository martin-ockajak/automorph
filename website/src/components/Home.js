import clsx from 'clsx'
import styles from './Home.module.css'
import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import Link from '@docusaurus/Link'
import React from 'react'
import homeImage from "../../../docs/images/home.jpg";

const features = [
  {
    title: 'Convenient',
    description: (
      <>
        Client-side or server-side remote API bindings are created automatically from public methods of existing API classes.
      </>
    ),
  },
  {
    title: 'Modular',
    description: (
      <>
        <a href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> protocol, <a href="https://en.wikipedia.org/wiki/Effect_system">effect</a> type, <a href="https://en.wikipedia.org/wiki/Transport_layer">transport</a> protocol and message <a href="https://en.wikipedia.org/wiki/File_format">format</a> can be freely combined by choosing appropriate plugins.
      </>
    ),
  },
  {
    title: 'Modular',
    description: (
      <>
        <a href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> protocol, <a href="https://en.wikipedia.org/wiki/Effect_system">effect</a> type, <a href="https://en.wikipedia.org/wiki/Transport_layer">transport</a> protocol and message <a href="https://en.wikipedia.org/wiki/File_format">format</a> can be freely combined by choosing appropriate plugins.
      </>
    ),
  },
  {
    title: 'Modular',
    description: (
      <>
        <a href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> protocol, <a href="https://en.wikipedia.org/wiki/Effect_system">effect</a> type, <a href="https://en.wikipedia.org/wiki/Transport_layer">transport</a> protocol and message <a href="https://en.wikipedia.org/wiki/File_format">format</a> can be freely combined by choosing appropriate plugins.
      </>
    ),
  },
  {
    title: 'Modular',
    description: (
      <>
        <a href="https://en.wikipedia.org/wiki/Remote_procedure_call">RPC</a> protocol, <a href="https://en.wikipedia.org/wiki/Effect_system">effect</a> type, <a href="https://en.wikipedia.org/wiki/Transport_layer">transport</a> protocol and message <a href="https://en.wikipedia.org/wiki/File_format">format</a> can be freely combined by choosing appropriate plugins.
      </>
    ),
  },
  {
    title: 'Clean',
    description: (
      <>
        Underlying transport protocol request and response metadata can be accessed using optional API abstractions.
      </>
    ),
  },
]

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
    color: '#000000'
  },
  button: {
    margin: '0rem 1rem 3rem 1rem',
    color: '#000000',
    fontSize: '1.5rem',
    '&:hover': {
      background: '#ffffff',
    }
  },
  features: {
    display: 'flex',
    alignItems: 'center',
    padding: '2rem 0'
  }
}

function Feature({title, description}) {
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
      <div style={{position: 'relative'}}>
        <img src={homeImage} alt={config.title} />
        <div style={style.headerText}>
	  <p className="hero__subtitle" style={style.subtitle}>{config.tagline}</p>
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
      <main>
        <Features />
      </main>
    </Layout>
  )
}

