/**
 * Hook useAuth — Re-export centralisé.
 *
 * Ce fichier existe pour deux raisons :
 *   1. Convention : les hooks personnalisés vivent dans src/hooks/
 *   2. Permet d'importer via  `import useAuth from '../hooks/useAuth'`
 *      plutôt que le chemin plus long vers AuthContext
 *
 * La logique réelle est dans AuthContext.jsx (co-localisée avec le Context
 * qu'elle consomme, ce qui est la convention React recommandée).
 *
 * USAGE :
 *   import useAuth from '../hooks/useAuth'
 *   // ou directement :
 *   import { useAuth } from '../context/AuthContext'
 *
 * Les deux imports sont équivalents. Choisir une convention et s'y tenir.
 */
export { useAuth as default } from '../context/AuthContext'
