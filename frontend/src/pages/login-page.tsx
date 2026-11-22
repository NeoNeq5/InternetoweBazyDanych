import { useState, type FormEvent } from "react"

import { DepartureTable } from "@/components/departure-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { formatTime, formatWeekday } from "@/lib/format"
import { useNow } from "@/lib/use-now"

const demoEmail = "anna.kowalska@pwr.edu.pl"

export function LoginPage({ onEnter }: { onEnter: () => void }) {
  const now = useNow()
  const [notice, setNotice] = useState("")

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const email = String(data.get("email") ?? "").trim()
    const password = String(data.get("password") ?? "")

    if (!email || !password) {
      setNotice("Podaj adres e-mail i hasło.")
      return
    }

    setNotice("")
    onEnter()
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-[minmax(0,1fr)_22rem]">
      <section className="order-2 border-t lg:order-1 lg:border-t-0 lg:border-r">
        <div className="flex items-end justify-between gap-4 border-b px-4 py-4 lg:px-6">
          <div>
            <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Tablica odjazdów
            </p>
            <h1 className="mt-1 text-3xl font-medium tracking-tight">
              Plac Grunwaldzki
            </h1>
          </div>
          <p className="text-right">
            <span className="block text-3xl font-medium tabular-nums">
              {formatTime(now)}
            </span>
            <span className="text-xs text-muted-foreground">
              {formatWeekday(now)}
            </span>
          </p>
        </div>
        <DepartureTable
          stopId="plac-grunwaldzki"
          stopName="Plac Grunwaldzki"
          now={now}
          limit={10}
        />
        <p className="border-t px-4 py-4 text-sm leading-6 text-muted-foreground lg:px-6">
          Konto trzyma saldo portfela, bilety czasowe i mandaty z kontroli.
          Hasło nie idzie jeszcze na serwer — po zalogowaniu otwiera się konto{" "}
          {demoEmail}.
        </p>
      </section>

      <section className="order-1 px-5 py-6 lg:order-2 lg:px-6 lg:py-8">
        <form className="flex flex-col gap-5" onSubmit={onSubmit}>
          <div>
            <p className="text-xs font-medium tracking-wide text-primary uppercase">
              Pasażer
            </p>
            <h2 className="mt-2 text-2xl font-medium tracking-tight">
              Logowanie
            </h2>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="email">E-mail</Label>
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              required
              defaultValue={demoEmail}
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="password">Hasło</Label>
            <Input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
            />
          </div>

          <Button type="submit" className="h-10">
            Zaloguj się
          </Button>

          <p className="min-h-5 text-sm text-muted-foreground" role="status">
            {notice}
          </p>
        </form>
      </section>
    </div>
  )
}
