import { Button } from "@/components/ui/button"
import { routeById } from "@/data/network"
import { formatDateTime, formatMoney } from "@/lib/format"
import { penaltyStatusLabel, useAccount } from "@/state/account"

export function PenaltiesPage() {
  const { penalties, balanceCents, pay } = useAccount()

  return (
    <div>
      <div className="px-4 py-4 lg:px-6">
        <h1 className="text-lg font-medium">Mandaty</h1>
        <p className="mt-1 max-w-xl text-sm leading-6 text-muted-foreground">
          Opłata schodzi z portfela. Na koncie jest {formatMoney(balanceCents)}.
          Anulowany mandat nie wymaga wpłaty.
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[52rem] text-sm">
          <caption className="sr-only">Lista mandatów</caption>
          <thead className="text-left text-xs text-muted-foreground">
            <tr className="border-y">
              <th className="px-4 py-2 font-medium lg:px-6">Numer</th>
              <th className="px-2 py-2 font-medium">Kontrola</th>
              <th className="px-2 py-2 font-medium">Wystawiono</th>
              <th className="px-2 py-2 font-medium">Termin</th>
              <th className="px-2 py-2 text-right font-medium">Kwota</th>
              <th className="px-2 py-2 font-medium">Status</th>
              <th className="px-4 py-2 lg:px-6" />
            </tr>
          </thead>
          <tbody>
            {penalties.map((penalty) => {
              const route = routeById(penalty.routeShortName)
              const shortfall = penalty.amountCents - balanceCents
              return (
                <tr
                  key={penalty.id}
                  className="border-b align-top hover:bg-muted"
                >
                  <td className="px-4 py-3 font-medium lg:px-6">
                    {penalty.penaltyNumber}
                  </td>
                  <td className="px-2 py-3">
                    <p>
                      Linia {penalty.routeShortName}
                      {route ? ` · ${route.longName}` : ""}
                    </p>
                    <p className="text-muted-foreground">
                      {penalty.validTicket ? "Bilet ważny" : "Bilet nieważny"}
                    </p>
                  </td>
                  <td className="px-2 py-3 whitespace-nowrap">
                    {formatDateTime(penalty.issuedAt)}
                  </td>
                  <td className="px-2 py-3 whitespace-nowrap">
                    {formatDateTime(penalty.dueDate)}
                  </td>
                  <td className="px-2 py-3 text-right tabular-nums">
                    {formatMoney(penalty.amountCents)}
                  </td>
                  <td className="px-2 py-3">
                    {penaltyStatusLabel(penalty.status)}
                  </td>
                  <td className="px-4 py-3 text-right lg:px-6">
                    {penalty.status === "ISSUED" ? (
                      <div className="flex flex-col items-end gap-1">
                        <Button className="h-8" onClick={() => pay(penalty.id)}>
                          Opłać
                        </Button>
                        {shortfall > 0 ? (
                          <p className="text-xs text-muted-foreground">
                            Brakuje {formatMoney(shortfall)}
                          </p>
                        ) : null}
                      </div>
                    ) : null}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
