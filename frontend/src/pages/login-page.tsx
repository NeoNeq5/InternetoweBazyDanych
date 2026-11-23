import { useState, type FormEvent } from "react"

import { useTheme } from "@/components/theme-provider"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { demoEmail, useAccount } from "@/state/account"

type AuthMode = "login" | "register"

export function LoginPage({ onEnter }: { onEnter: () => void }) {
  const { theme, setTheme } = useTheme()
  const { signIn, register } = useAccount()

  const [mode, setMode] = useState<AuthMode>("login")
  const [email, setEmail] = useState(demoEmail)
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [error, setError] = useState("")

  function switchMode(next: AuthMode) {
    setMode(next)
    setError("")
    setPassword("")
    setConfirmPassword("")
    setEmail(next === "login" ? demoEmail : "")
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedEmail = email.trim()

    if (!trimmedEmail || !password) {
      setError("Podaj adres e-mail i hasło.")
      return
    }

    if (mode === "register") {
      if (password.length < 8) {
        setError("Hasło musi mieć co najmniej 8 znaków.")
        return
      }
      if (password !== confirmPassword) {
        setError("Hasła nie są takie same.")
        return
      }
      const result = register(trimmedEmail, password)
      if (!result.ok) {
        setError(result.error)
        return
      }
      setError("")
      onEnter()
      return
    }

    const result = signIn(trimmedEmail, password)
    if (!result.ok) {
      setError(result.error)
      return
    }

    setError("")
    onEnter()
  }

  return (
    <div className="flex min-h-svh flex-col bg-background text-foreground">
      <header className="border-b bg-background">
        <div className="flex items-center justify-between gap-4 px-4 py-3 lg:px-6">
          <div className="leading-tight">
            <p className="text-sm font-medium">Transit</p>
            <p className="text-xs text-muted-foreground">Konto pasażera</p>
          </div>
          <Button
            variant="ghost"
            className="h-9"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          >
            {theme === "dark" ? "Jasny" : "Ciemny"}
          </Button>
        </div>
      </header>

      <main className="grid flex-1 place-items-center px-4 py-10">
        <section className="w-full max-w-sm border bg-card">
          <div className="border-b px-6 py-5">
            <p className="text-xs font-medium tracking-wide text-primary uppercase">
              {mode === "login" ? "Pasażer" : "Nowe konto"}
            </p>
            <h1 className="mt-1.5 text-2xl font-medium tracking-tight">
              {mode === "login" ? "Logowanie" : "Rejestracja"}
            </h1>
          </div>

          <form className="flex flex-col gap-4 px-6 py-6" onSubmit={onSubmit}>
            <div className="flex flex-col gap-2">
              <Label htmlFor="email">E-mail</Label>
              <Input
                id="email"
                name="email"
                type="email"
                autoComplete={mode === "login" ? "username" : "email"}
                placeholder="jan.nowak@pwr.edu.pl"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Hasło</Label>
              <Input
                id="password"
                name="password"
                type="password"
                autoComplete={
                  mode === "login" ? "current-password" : "new-password"
                }
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>

            {mode === "register" ? (
              <div className="flex flex-col gap-2">
                <Label htmlFor="confirm-password">Powtórz hasło</Label>
                <Input
                  id="confirm-password"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                />
              </div>
            ) : null}

            {error ? (
              <p className="text-sm text-destructive" role="alert">
                {error}
              </p>
            ) : null}

            <div className="mt-1 flex flex-col gap-2">
              <Button type="submit" className="h-10 w-full">
                {mode === "login" ? "Zaloguj się" : "Utwórz konto"}
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-10 w-full"
                onClick={() =>
                  switchMode(mode === "login" ? "register" : "login")
                }
              >
                {mode === "login" ? "Zarejestruj się" : "Powrót do logowania"}
              </Button>
            </div>
          </form>

          <p className="border-t bg-muted/40 px-6 py-3 text-xs leading-5 text-muted-foreground">
            {mode === "login"
              ? `Konto testowe: ${demoEmail} (dowolne hasło).`
              : "Hasło musi mieć co najmniej 8 znaków."}
          </p>
        </section>
      </main>
    </div>
  )
}
