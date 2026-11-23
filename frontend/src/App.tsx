import { useState } from "react"

import { DeskFrame, type SectionId } from "@/components/desk-frame"
import { LoginPage } from "@/pages/login-page"
import { PenaltiesPage } from "@/pages/penalties-page"
import { PulpitPage } from "@/pages/pulpit-page"
import { StopsPage } from "@/pages/stops-page"
import { TicketsPage } from "@/pages/tickets-page"
import { WalletPage } from "@/pages/wallet-page"
import { AccountProvider, useAccount } from "@/state/account"
import { RoutesProvider } from "@/state/routes"
import { StopsProvider } from "@/state/stops"

export function App() {
  return (
    <AccountProvider>
      <RoutesProvider>
        <StopsProvider>
          <Root />
        </StopsProvider>
      </RoutesProvider>
    </AccountProvider>
  )
}

function Root() {
  const [entered, setEntered] = useState(false)
  const [section, setSection] = useState<SectionId>("pulpit")
  const { clearNotice } = useAccount()

  if (!entered) {
    return <LoginPage onEnter={() => setEntered(true)} />
  }

  function onSection(next: SectionId) {
    setSection(next)
    clearNotice()
  }

  function onLogout() {
    clearNotice()
    setSection("pulpit")
    setEntered(false)
  }

  return (
    <DeskFrame
      section={section}
      onSection={onSection}
      onLogout={onLogout}
    >
      {section === "pulpit" ? <PulpitPage onSection={onSection} /> : null}
      {section === "portfel" ? <WalletPage /> : null}
      {section === "bilety" ? <TicketsPage /> : null}
      {section === "przystanki" ? <StopsPage /> : null}
      {section === "mandaty" ? <PenaltiesPage /> : null}
    </DeskFrame>
  )
}

export default App
