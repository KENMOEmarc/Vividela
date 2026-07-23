import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import 'react-toastify/dist/ReactToastify.css'
import './index.css'

import App from './App'

/*
 * createRoot (React 18) remplace ReactDOM.render() (React 17).
 * Permet le rendu concurrent (Concurrent Mode) : React peut
 * interrompre et reprendre le rendu pour rester réactif.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>
)
