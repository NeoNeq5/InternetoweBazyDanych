import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react"

import { fetchRoutes, type RouteDto } from "@/api/routes"

type RoutesState = {
  routes: RouteDto[]
  loading: boolean
  error: string | null
  routeById: (id: string) => RouteDto | undefined
  refresh: () => void
}

const RoutesContext = createContext<RoutesState | null>(null)

export function RoutesProvider({ children }: { children: ReactNode }) {
  const [routes, setRoutes] = useState<RouteDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)

    fetchRoutes(controller.signal)
      .then((data) => {
        setRoutes(data)
        setLoading(false)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setRoutes([])
        setLoading(false)
        setError(err instanceof Error ? err.message : "Błąd pobierania tras")
      })

    return () => controller.abort()
  }, [tick])

  const value: RoutesState = {
    routes,
    loading,
    error,
    routeById: (id) => routes.find((route) => route.id === id),
    refresh: () => setTick((value) => value + 1),
  }

  return <RoutesContext.Provider value={value}>{children}</RoutesContext.Provider>
}

export function useRoutes() {
  const context = useContext(RoutesContext)
  if (!context) {
    throw new Error("useRoutes must be used within RoutesProvider")
  }
  return context
}
