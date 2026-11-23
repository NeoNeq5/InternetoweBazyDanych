import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react"

import { fetchStops, type StopDto } from "@/api/stops"

type StopsState = {
  stops: StopDto[]
  loading: boolean
  error: string | null
  stopById: (id: string) => StopDto | undefined
}

const StopsContext = createContext<StopsState | null>(null)

export function StopsProvider({ children }: { children: ReactNode }) {
  const [stops, setStops] = useState<StopDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(null)

    fetchStops(controller.signal)
      .then((data) => {
        setStops(data)
        setLoading(false)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setStops([])
        setLoading(false)
        setError(
          err instanceof Error ? err.message : "Błąd pobierania przystanków"
        )
      })

    return () => controller.abort()
  }, [])

  return (
    <StopsContext.Provider
      value={{
        stops,
        loading,
        error,
        stopById: (id) => stops.find((stop) => stop.id === id),
      }}
    >
      {children}
    </StopsContext.Provider>
  )
}

export function useStops() {
  const context = useContext(StopsContext)
  if (!context) {
    throw new Error("useStops must be used within StopsProvider")
  }
  return context
}
