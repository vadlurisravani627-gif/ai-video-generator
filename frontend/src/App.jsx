import { useEffect, useState } from 'react'
import './App.css'

const API = 'http://localhost:8080/api/projects'

const ORDER = ['PENDING', 'SCRIPT_READY', 'AUDIO_READY', 'IMAGES_READY', 'RENDERING', 'DONE']

const STEPS = [
  { label: 'Writing the script', doneAt: 1 },
  { label: 'Recording the voice', doneAt: 2 },
  { label: 'Creating the images', doneAt: 3 },
  { label: 'Rendering the video', doneAt: 5 },
]

function App() {
  const [topic, setTopic] = useState('')
  const [project, setProject] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const status = project ? project.status : null
  const failed = status === 'FAILED'
  const done = status === 'DONE'
  const working = project !== null && !failed && !done
  const statusIndex = ORDER.indexOf(status)

  useEffect(() => {
    if (!project || !working) return
    const timer = setInterval(async () => {
      try {
        const res = await fetch(`${API}/${project.id}`)
        if (res.ok) setProject(await res.json())
      } catch {
        // network hiccup, try again on the next tick
      }
    }, 3000)
    return () => clearInterval(timer)
  }, [project?.id, working])

  async function createVideo(e) {
    e.preventDefault()
    if (!topic.trim()) return
    setError('')
    setLoading(true)
    setProject(null)
    try {
      const res = await fetch(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topic: topic.trim() }),
      })
      if (!res.ok) throw new Error('Server returned ' + res.status)
      setProject(await res.json())
    } catch (err) {
      setError('Could not reach the backend (' + err.message + '). Is it running on port 8080?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page">
      <main className="card">
        <h1>AI Short Video Generator</h1>
        <p className="subtitle">Type a topic and get a narrated short video.</p>

        <form onSubmit={createVideo} className="form">
          <input
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder="e.g. a dog discovering the ocean"
            maxLength={120}
            disabled={loading || working}
          />
          <button type="submit" disabled={loading || working || !topic.trim()}>
            {loading ? 'Starting...' : 'Generate'}
          </button>
        </form>

        {error && <p className="error">{error}</p>}

        {project && (
          <section className="progress">
            <ol>
              {STEPS.map((step, i) => {
                const stepDone = statusIndex >= step.doneAt
                const previousDone = i === 0 || statusIndex >= STEPS[i - 1].doneAt
                const active = !failed && !stepDone && previousDone
                const cls = stepDone ? 'step done' : active ? 'step active' : 'step'
                return (
                  <li key={step.label} className={cls}>
                    <span className="dot" />
                    {step.label}
                  </li>
                )
              })}
            </ol>
            {working && <p className="hint">This takes a few minutes. Keep this page open.</p>}
            {failed && (
              <p className="error">
                Something went wrong while making the video. Check the backend terminal for the error and try again.
              </p>
            )}
          </section>
        )}

        {done && (
          <section className="result">
            <video controls autoPlay src={`${API}/${project.id}/video`} />
            <details>
              <summary>Show the script</summary>
              <p>{project.script}</p>
            </details>
            <button className="secondary" onClick={() => { setProject(null); setTopic('') }}>
              Make another video
            </button>
          </section>
        )}
      </main>
    </div>
  )
}

export default App 