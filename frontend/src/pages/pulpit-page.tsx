import { useState } from "react"

import { DepartureTable } from "@/components/departure-table"
import type { SectionId } from "@/components/desk-frame"
import { LineBadge } from "@/components/line-badge"
import { Button } from "@/components/ui/button"
import { routeById, stopById } from "@/data/network"
import { formatDateTime, formatMoney, formatRemaining } from "@/lib/format"
import { useNow } from "@/lib/use-now"
import { cn } from "cn"
import { ticketTypeById, useAccount } from "@/state/account"

export function PulpitPage({
  onSection,
}: {
  onSection: (section: SectionId) => void
}) {
  const now = useNow()
  const { balanceCents, tickets, penalties, favoriteStopIds } = useAccount()
  const favorites = favoriteStopIds
    .map((id) => stopById(id))
    .filter((stop) => stop !== undefined)
  const [selectedId, setSelectedId] = useState(
    favorites[0]?.id ?? "plac-grunwaldzki"
  )
  const selected =
    favorites.find((stop) => stop.id === selectedId) ?? favorites[0]

  const active = tickets
    .filter((ticket) => ticket.expiresAt && new Date(ticket.expiresAt) > now)
    .sort(
      (left, right) =>
        new Date(left.expiresAt ?? 0).getTime() -
        new Date(right.expiresAt ?? 0).getTime()
    )
  const current = active[0]
  const currentType = current ? ticketTypeById(current.ticketTypeId) : undefined
  const currentRoute = current?.routeId ? routeById(current.routeId) : undefined
  const openCount = tickets.filter((ticket) => !ticket.validatedAt).length
  const issued = penalties.find((penalty) => penalty.status === "ISSUED")

  return (
    <div className="lg:grid lg:min-h-[calc(100svh-6.75rem)] lg:grid-cols-12">
      <section className="border-b lg:col-span-8 lg:border-r lg:border-b-0">
        <div className="flex items-baseline justify-between gap-4 px-4 py-4 lg:px-6">
          <h1 className="text-lg font-medium">Skasowany bilet</h1>
          <p className="text-xs text-muted-foreground">ważność od skasowania</p>
        </div>
        {current && currentType && current.expiresAt ? (
          <div className="grid gap-6 px-4 pb-6 sm:grid-cols-[minmax(0,1fr)_auto] lg:px-6">
            <div>
              <p className="font-mono text-sm">{current.code}</p>
              <p className="mt-2 text-3xl font-medium tracking-tight">
                {currentType.name}
              </p>
              <p className="mt-3 flex flex-wrap items-center gap-2 text-sm">
                {current.routeId ? (
                  <LineBadge routeId={current.routeId} />
                ) : null}
                <span>
                  {currentRoute ? currentRoute.longName : "Linia nieprzypisana"}
                </span>
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                do {formatDateTime(current.expiresAt)}
              </p>
              {active.length > 1 ? (
                <p className="mt-3 text-sm text-muted-foreground">
                  Jeszcze {active.length - 1}{" "}
                  {active.length - 1 === 1 ? "bilet ważny" : "bilety ważne"}.
                </p>
              ) : null}
            </div>
            <p className="text-4xl font-medium tracking-tight tabular-nums sm:text-5xl">
              {formatRemaining(
                new Date(current.expiresAt).getTime() - now.getTime()
              )}
            </p>
          </div>
        ) : (
          <div className="px-4 pb-6 lg:px-6">
            <p className="text-2xl font-medium tracking-tight">
              Brak biletu w czasie ważności.
            </p>
            <p className="mt-2 max-w-md text-sm leading-6 text-muted-foreground">
              {openCount > 0
                ? `${openCount === 1 ? "Jest 1 nieskasowany bilet" : `Są ${openCount} nieskasowane bilety`}. Skasuj go na linii albo kup nowy.`
                : "Kup bilet czasowy i skasuj go przy wsiadaniu."}
            </p>
            <Button className="mt-4 h-10" onClick={() => onSection("bilety")}>
              Bilety
            </Button>
          </div>
        )}
      </section>

      <aside className="border-b lg:col-span-4 lg:border-b-0">
        <div className="border-b px-4 py-4 lg:px-5">
          <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            Portfel
          </p>
          <p className="mt-2 text-3xl font-medium tabular-nums">
            {formatMoney(balanceCents)}
          </p>
          <Button
            variant="outline"
            className="mt-4 h-9"
            onClick={() => onSection("portfel")}
          >
            Doładuj
          </Button>
        </div>
        <div className="px-4 py-4 lg:px-5">
          <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            Mandat
          </p>
          {issued ? (
            <>
              <p className="mt-2 font-medium">{issued.penaltyNumber}</p>
              <p className="mt-1 text-sm tabular-nums">
                {formatMoney(issued.amountCents)}
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Termin {formatDateTime(issued.dueDate)}. Linia{" "}
                {issued.routeShortName}, bilet nieważny.
              </p>
              <Button
                variant="outline"
                className="mt-4 h-9"
                onClick={() => onSection("mandaty")}
              >
                Opłać
              </Button>
            </>
          ) : (
            <p className="mt-2 text-sm text-muted-foreground">
              Brak mandatu ze statusem wystawiony.
            </p>
          )}
        </div>
      </aside>

      <section className="border-t lg:col-span-12">
        <div className="flex flex-wrap items-end justify-between gap-3 px-4 py-4 lg:px-6">
          <div>
            <h2 className="text-lg font-medium">Odjazdy</h2>
            <p className="text-sm text-muted-foreground">
              {selected ? selected.name : "Brak ulubionego przystanku"}
            </p>
          </div>
          {favorites.length > 0 ? (
            <div className="flex flex-wrap gap-1">
              {favorites.map((stop) => (
                <Button
                  key={stop.id}
                  variant="ghost"
                  className={cn(
                    "h-8 px-2",
                    selected?.id === stop.id && "bg-muted text-foreground"
                  )}
                  aria-pressed={selected?.id === stop.id}
                  onClick={() => setSelectedId(stop.id)}
                >
                  {stop.name}
                </Button>
              ))}
            </div>
          ) : (
            <Button variant="outline" onClick={() => onSection("przystanki")}>
              Dodaj przystanek
            </Button>
          )}
        </div>
        {selected ? (
          <DepartureTable
            stopId={selected.id}
            stopName={selected.name}
            now={now}
          />
        ) : null}
      </section>
    </div>
  )
}
