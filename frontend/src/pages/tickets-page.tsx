import { useState } from "react"

import { LineBadge } from "@/components/line-badge"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { NativeSelect } from "@/components/ui/input"
import {
  formatDateTime,
  formatMoney,
  formatRemaining,
  formatValidity,
} from "@/lib/format"
import { useNow } from "@/lib/use-now"
import {
  ticketTypeById,
  ticketTypes,
  useAccount,
  type PurchasedTicket,
} from "@/state/account"
import { useRoutes } from "@/state/routes"

export function TicketsPage() {
  const now = useNow()
  const { tickets, buy } = useAccount()
  const { routes, loading, error } = useRoutes()

  return (
    <div className="lg:grid lg:min-h-[calc(100svh-6.75rem)] lg:grid-cols-12">
      <section className="border-b lg:col-span-7 lg:border-r lg:border-b-0">
        <div className="px-4 py-4 lg:px-6">
          <h1 className="text-lg font-medium">Taryfa czasowa</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cena schodzi z portfela. Bilet zaczyna się liczyć po skasowaniu.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[32rem] text-sm">
            <caption className="sr-only">Cennik biletów</caption>
            <thead className="text-left text-xs text-muted-foreground">
              <tr className="border-y">
                <th className="px-4 py-2 font-medium lg:px-6">Nazwa</th>
                <th className="px-2 py-2 font-medium">Ważność</th>
                <th className="px-2 py-2 text-right font-medium">Cena</th>
                <th className="px-4 py-2 lg:px-6" />
              </tr>
            </thead>
            <tbody>
              {ticketTypes.map((tariff) => (
                <tr key={tariff.id} className="border-b hover:bg-muted">
                  <td className="px-4 py-2.5 font-medium lg:px-6">
                    {tariff.name}
                  </td>
                  <td className="px-2 py-2.5 text-muted-foreground">
                    {formatValidity(tariff.validityMinutes)}
                  </td>
                  <td className="px-2 py-2.5 text-right tabular-nums">
                    {formatMoney(tariff.priceCents)}
                  </td>
                  <td className="px-4 py-2.5 text-right lg:px-6">
                    <Button
                      className="h-8"
                      disabled={!tariff.active}
                      onClick={() => buy(tariff.id)}
                    >
                      Kup
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="lg:col-span-5">
        <div className="px-4 py-4 lg:px-6">
          <h2 className="text-lg font-medium">Kupione</h2>
          {loading ? (
            <p className="mt-1 text-sm text-muted-foreground">
              Ładowanie linii z bazy…
            </p>
          ) : null}
          {error ? (
            <p className="mt-1 text-sm text-destructive">{error}</p>
          ) : null}
          {!loading && !error ? (
            <p className="mt-1 text-sm text-muted-foreground">
              {routes.length} linii z rozkładu GTFS Wrocław.
            </p>
          ) : null}
        </div>
        <ul>
          {tickets.map((ticket) => (
            <TicketRow key={ticket.id} ticket={ticket} now={now} />
          ))}
        </ul>
      </section>
    </div>
  )
}

function TicketRow({ ticket, now }: { ticket: PurchasedTicket; now: Date }) {
  const { validate } = useAccount()
  const { routes, routeById, loading } = useRoutes()
  const [routeId, setRouteId] = useState("")
  const tariff = ticketTypeById(ticket.ticketTypeId)
  const route = ticket.routeId ? routeById(ticket.routeId) : undefined
  const expires = ticket.expiresAt ? new Date(ticket.expiresAt) : null
  const active = expires ? expires > now : false

  let status = "Nieskasowany"
  if (ticket.validatedAt && active && expires) {
    status = `Ważny jeszcze ${formatRemaining(expires.getTime() - now.getTime())}`
  } else if (ticket.validatedAt) {
    status = "Po terminie"
  }

  return (
    <li className="border-t px-4 py-4 lg:px-6">
      <div className="flex items-baseline justify-between gap-3">
        <p className="font-mono text-sm">{ticket.code}</p>
        <p className="text-sm tabular-nums">
          {formatMoney(ticket.pricePaidCents)}
        </p>
      </div>
      <p className="mt-1 text-sm font-medium">{tariff?.name ?? "Bilet"}</p>
      <p className="mt-1 text-sm text-muted-foreground">{status}</p>
      {route ? (
        <p className="mt-2 flex items-center gap-2 text-sm">
          <LineBadge routeId={route.id} />
          <span>{route.longName}</span>
        </p>
      ) : null}
      <p className="mt-1 text-xs text-muted-foreground">
        Kupiony {formatDateTime(ticket.purchasedAt)}
      </p>
      {!ticket.validatedAt ? (
        <form
          className="mt-3 flex flex-col gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            validate(ticket.id, routeId)
          }}
        >
          <Label htmlFor={`route-${ticket.id}`}>Linia skasowania</Label>
          <NativeSelect
            id={`route-${ticket.id}`}
            value={routeId}
            onChange={(event) => setRouteId(event.target.value)}
            disabled={loading || routes.length === 0}
          >
            <option value="">
              {loading ? "Ładowanie linii…" : "Wybierz linię"}
            </option>
            {routes.map((item) => (
              <option key={item.id} value={item.id}>
                {item.shortName} · {item.longName}
              </option>
            ))}
          </NativeSelect>
          <Button
            type="submit"
            className="h-10"
            disabled={loading || routes.length === 0}
          >
            Skasuj
          </Button>
        </form>
      ) : null}
    </li>
  )
}
