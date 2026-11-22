import { Button } from "@/components/ui/button"
import { formatDateTime, formatMoney } from "@/lib/format"
import { ticketTypeById, transactionLabel, useAccount } from "@/state/account"

const topUpAmounts = [2000, 5000, 10000]

export function WalletPage() {
  const { balanceCents, transactions, tickets, penalties, topUp } = useAccount()

  return (
    <div className="lg:grid lg:min-h-[calc(100svh-6.75rem)] lg:grid-cols-12">
      <section className="border-b px-4 py-5 lg:col-span-4 lg:border-r lg:border-b-0 lg:px-6">
        <h1 className="text-lg font-medium">Portfel</h1>
        <p className="mt-4 text-4xl font-medium tabular-nums">
          {formatMoney(balanceCents)}
        </p>
        <p className="mt-3 max-w-xs text-sm leading-6 text-muted-foreground">
          Doładowanie księguje się od razu. Bilet i mandat schodzą z tego salda.
        </p>
        <div className="mt-5 flex max-w-xs flex-col gap-2">
          {topUpAmounts.map((amount) => (
            <Button
              key={amount}
              variant="outline"
              className="h-10 justify-start tabular-nums"
              onClick={() => topUp(amount)}
            >
              Doładuj {formatMoney(amount)}
            </Button>
          ))}
        </div>
      </section>

      <section className="lg:col-span-8">
        <h2 className="px-4 py-4 text-lg font-medium lg:px-6">Operacje</h2>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[36rem] text-sm">
            <caption className="sr-only">Historia portfela</caption>
            <thead className="text-left text-xs text-muted-foreground">
              <tr className="border-y">
                <th className="px-4 py-2 font-medium lg:px-6">Kiedy</th>
                <th className="px-2 py-2 font-medium">Rodzaj</th>
                <th className="px-2 py-2 font-medium">Opis</th>
                <th className="px-4 py-2 text-right font-medium lg:px-6">
                  Kwota
                </th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((transaction) => (
                <tr key={transaction.id} className="border-b hover:bg-muted">
                  <td className="px-4 py-2.5 whitespace-nowrap text-muted-foreground lg:px-6">
                    {formatDateTime(transaction.createdAt)}
                  </td>
                  <td className="px-2 py-2.5">
                    {transactionLabel(transaction.type)}
                  </td>
                  <td className="px-2 py-2.5">{describe(transaction)}</td>
                  <td className="px-4 py-2.5 text-right font-medium tabular-nums lg:px-6">
                    {formatMoney(transaction.amountCents)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )

  function describe(transaction: (typeof transactions)[number]) {
    if (transaction.type === "TOP_UP") {
      return "Wpłata na konto"
    }
    if (transaction.type === "TICKET_PURCHASE") {
      const ticket = tickets.find((item) => item.id === transaction.ticketId)
      const tariff = ticket ? ticketTypeById(ticket.ticketTypeId) : undefined
      return ticket ? `${tariff?.name ?? "Bilet"} · ${ticket.code}` : "Bilet"
    }
    const penalty = penalties.find((item) => item.id === transaction.penaltyId)
    return penalty ? penalty.penaltyNumber : "Mandat"
  }
}
