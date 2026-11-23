import type { ReactNode } from "react"

import { Button } from "@/components/ui/button"
import { formatMoney } from "@/lib/format"
import { useAccount } from "@/state/account"
import { useTheme } from "@/components/theme-provider"
import { cn } from "cn"

const sections = [
  { id: "pulpit", label: "Pulpit" },
  { id: "portfel", label: "Portfel" },
  { id: "bilety", label: "Bilety" },
  { id: "przystanki", label: "Mapa" },
  { id: "mandaty", label: "Mandaty" },
] as const

export type SectionId = (typeof sections)[number]["id"]

export function DeskFrame({
  section,
  onSection,
  onLogout,
  children,
}: {
  section: SectionId
  onSection: (section: SectionId) => void
  onLogout: () => void
  children: ReactNode
}) {
  const { email, balanceCents, penalties, notice, clearNotice } = useAccount()
  const { theme, setTheme } = useTheme()
  const issued = penalties.filter((item) => item.status === "ISSUED").length

  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="sticky top-0 z-10 border-b bg-background">
        <div className="flex items-center justify-between gap-4 px-4 py-3 lg:px-6">
          <div className="min-w-0 leading-tight">
            <p className="text-sm font-medium">Transit</p>
            <p className="truncate text-xs text-muted-foreground">{email}</p>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Button
              variant="outline"
              className="h-9 tabular-nums"
              onClick={() => onSection("portfel")}
            >
              {formatMoney(balanceCents)}
            </Button>
            <Button
              variant="ghost"
              className="h-9"
              onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            >
              {theme === "dark" ? "Jasny" : "Ciemny"}
            </Button>
            <Button variant="ghost" className="h-9" onClick={onLogout}>
              Wyloguj
            </Button>
          </div>
        </div>
        <nav
          className="flex gap-1 overflow-x-auto border-t px-2"
          aria-label="Konto"
        >
          {sections.map((item) => {
            const active = item.id === section
            return (
              <Button
                key={item.id}
                variant="ghost"
                className={cn(
                  "h-10 rounded-none px-3",
                  active && "bg-muted text-foreground"
                )}
                aria-current={active ? "page" : undefined}
                onClick={() => onSection(item.id)}
              >
                {item.label}
                {item.id === "mandaty" && issued > 0 ? ` ${issued}` : ""}
              </Button>
            )
          })}
        </nav>
        {notice ? (
          <div className="flex items-start justify-between gap-4 border-t bg-accent px-4 py-2 text-sm text-accent-foreground lg:px-6">
            <p role="status">{notice}</p>
            <button
              type="button"
              className="shrink-0 underline-offset-2 hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              onClick={clearNotice}
            >
              Zamknij
            </button>
          </div>
        ) : null}
      </header>
      <main>{children}</main>
    </div>
  )
}
