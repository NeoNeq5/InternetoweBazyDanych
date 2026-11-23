import {
  createContext,
  useContext,
  useMemo,
  useReducer,
  type ReactNode,
} from "react"

import { formatMoney } from "@/lib/format"

export type TicketType = {
  id: string
  name: string
  priceCents: number
  validityMinutes: number
  active: boolean
}

export const ticketTypes: TicketType[] = [
  {
    id: "15",
    name: "15 minut",
    priceCents: 440,
    validityMinutes: 15,
    active: true,
  },
  {
    id: "30",
    name: "30 minut",
    priceCents: 560,
    validityMinutes: 30,
    active: true,
  },
  {
    id: "60",
    name: "60 minut",
    priceCents: 680,
    validityMinutes: 60,
    active: true,
  },
  {
    id: "90",
    name: "90 minut",
    priceCents: 840,
    validityMinutes: 90,
    active: true,
  },
  {
    id: "24h",
    name: "24 godziny",
    priceCents: 1600,
    validityMinutes: 1440,
    active: true,
  },
]

export type PurchasedTicket = {
  id: string
  ticketTypeId: string
  code: string
  pricePaidCents: number
  purchasedAt: string
  validatedAt: string | null
  expiresAt: string | null
  routeId: string | null
}

export type TransactionType = "TOP_UP" | "TICKET_PURCHASE" | "PENALTY_PAYMENT"

export type WalletTransaction = {
  id: string
  type: TransactionType
  amountCents: number
  createdAt: string
  ticketId: string | null
  penaltyId: string | null
}

export type PenaltyStatus = "ISSUED" | "PAID" | "CANCELLED"

export type Penalty = {
  id: string
  penaltyNumber: string
  amountCents: number
  status: PenaltyStatus
  dueDate: string
  issuedAt: string
  routeShortName: string
  validTicket: boolean
}

export type SearchEntry = {
  id: string
  query: string
  searchedAt: string
}

export const demoEmail = "anna.kowalska@pwr.edu.pl"

type State = {
  email: string
  role: "PASSENGER"
  balanceCents: number
  tickets: PurchasedTicket[]
  transactions: WalletTransaction[]
  penalties: Penalty[]
  favoriteStopIds: string[]
  searches: SearchEntry[]
  notice: string
}

type AccountRecord = {
  password: string | null
  state: State
}

type Store = {
  activeEmail: string
  accounts: Record<string, AccountRecord>
}

type Action =
  | { type: "topup"; amountCents: number; at: string }
  | { type: "buy"; ticketTypeId: string; at: string }
  | { type: "validate"; ticketId: string; routeId: string; at: string }
  | { type: "pay"; penaltyId: string; at: string }
  | { type: "toggle-favorite"; stopId: string }
  | { type: "search"; query: string; at: string }
  | { type: "clear-notice" }
  | { type: "sign-in"; email: string }
  | { type: "register"; email: string; password: string }

function atPast(days: number, hour: number, minute: number) {
  const date = new Date()
  date.setDate(date.getDate() - days)
  date.setHours(hour, minute, 0, 0)
  if (date.getTime() > Date.now()) {
    date.setDate(date.getDate() - 1)
  }
  return date.toISOString()
}

function emptyAccount(email: string, notice = ""): State {
  return {
    email,
    role: "PASSENGER",
    balanceCents: 0,
    tickets: [],
    transactions: [],
    penalties: [],
    favoriteStopIds: [],
    searches: [],
    notice,
  }
}

function seed(email = demoEmail): State {
  const now = Date.now()
  return {
    email,
    role: "PASSENGER",
    balanceCents: 2320,
    tickets: [
      {
        id: "t1",
        ticketTypeId: "60",
        code: "TR-4F29C1",
        pricePaidCents: 680,
        purchasedAt: new Date(now - 38 * 60 * 1000).toISOString(),
        validatedAt: new Date(now - 37 * 60 * 1000).toISOString(),
        expiresAt: new Date(now + 23 * 60 * 1000).toISOString(),
        routeId: "4",
      },
      {
        id: "t2",
        ticketTypeId: "15",
        code: "TR-88C014",
        pricePaidCents: 440,
        purchasedAt: new Date(now - 2 * 60 * 60 * 1000).toISOString(),
        validatedAt: null,
        expiresAt: null,
        routeId: null,
      },
      {
        id: "t3",
        ticketTypeId: "30",
        code: "TR-19AA02",
        pricePaidCents: 560,
        purchasedAt: atPast(1, 8, 12),
        validatedAt: atPast(1, 8, 14),
        expiresAt: atPast(1, 8, 44),
        routeId: "17",
      },
    ],
    transactions: [
      {
        id: "w1",
        type: "TICKET_PURCHASE",
        amountCents: -680,
        createdAt: new Date(now - 38 * 60 * 1000).toISOString(),
        ticketId: "t1",
        penaltyId: null,
      },
      {
        id: "w2",
        type: "TICKET_PURCHASE",
        amountCents: -440,
        createdAt: new Date(now - 2 * 60 * 60 * 1000).toISOString(),
        ticketId: "t2",
        penaltyId: null,
      },
      {
        id: "w3",
        type: "TICKET_PURCHASE",
        amountCents: -560,
        createdAt: atPast(1, 8, 12),
        ticketId: "t3",
        penaltyId: null,
      },
      {
        id: "w4",
        type: "TOP_UP",
        amountCents: 4000,
        createdAt: atPast(3, 17, 40),
        ticketId: null,
        penaltyId: null,
      },
    ],
    penalties: [
      {
        id: "p1",
        penaltyNumber: "WR/2026/10483",
        amountCents: 20000,
        status: "ISSUED",
        dueDate: new Date(now + 14 * 24 * 60 * 60 * 1000).toISOString(),
        issuedAt: new Date(now - 6 * 60 * 60 * 1000).toISOString(),
        routeShortName: "8",
        validTicket: false,
      },
      {
        id: "p2",
        penaltyNumber: "WR/2025/8831",
        amountCents: 16000,
        status: "PAID",
        dueDate: atPast(40, 23, 59),
        issuedAt: atPast(48, 16, 5),
        routeShortName: "A",
        validTicket: false,
      },
    ],
    favoriteStopIds: [],
    searches: [
      { id: "s1", query: "Hala Stulecia", searchedAt: atPast(0, 7, 30) },
      { id: "s2", query: "Kromera", searchedAt: atPast(1, 18, 2) },
      { id: "s3", query: "Renoma", searchedAt: atPast(2, 12, 15) },
    ],
    notice: "",
  }
}

function ticketCode(at: string, typeId: string) {
  const stamp = new Date(at).getTime().toString(36).toUpperCase()
  return `TR-${stamp.slice(-4)}${typeId.toUpperCase()}`
}

function initialStore(): Store {
  return {
    activeEmail: demoEmail,
    accounts: {
      [demoEmail]: {
        password: null,
        state: seed(demoEmail),
      },
    },
  }
}

function storeReducer(store: Store, action: Action): Store {
  if (action.type === "sign-in") {
    const email = action.email.trim().toLowerCase()
    const existing = store.accounts[email]
    if (existing) {
      return {
        ...store,
        activeEmail: email,
        accounts: {
          ...store.accounts,
          [email]: {
            ...existing,
            state: { ...existing.state, notice: "" },
          },
        },
      }
    }
    return {
      activeEmail: email,
      accounts: {
        ...store.accounts,
        [email]: {
          password: null,
          state: seed(email),
        },
      },
    }
  }

  if (action.type === "register") {
    const email = action.email.trim().toLowerCase()
    return {
      activeEmail: email,
      accounts: {
        ...store.accounts,
        [email]: {
          password: action.password,
          state: emptyAccount(email, `Utworzono konto ${email}.`),
        },
      },
    }
  }

  const current = store.accounts[store.activeEmail]
  if (!current) {
    return store
  }
  const nextState = accountReducer(current.state, action)
  if (nextState === current.state) {
    return store
  }
  return {
    ...store,
    accounts: {
      ...store.accounts,
      [store.activeEmail]: {
        ...current,
        state: nextState,
      },
    },
  }
}

type AccountAction = Exclude<Action, { type: "sign-in" } | { type: "register" }>

function accountReducer(state: State, action: AccountAction): State {
  switch (action.type) {
    case "clear-notice":
      return { ...state, notice: "" }
    case "topup": {
      const transaction: WalletTransaction = {
        id: `w-${action.at}`,
        type: "TOP_UP",
        amountCents: action.amountCents,
        createdAt: action.at,
        ticketId: null,
        penaltyId: null,
      }
      return {
        ...state,
        balanceCents: state.balanceCents + action.amountCents,
        transactions: [transaction, ...state.transactions],
        notice: `Portfel zwiększony o ${formatMoney(action.amountCents)}.`,
      }
    }
    case "buy": {
      const tariff = ticketTypes.find(
        (item) => item.id === action.ticketTypeId && item.active
      )
      if (!tariff) {
        return { ...state, notice: "Ta taryfa jest wyłączona." }
      }
      if (state.balanceCents < tariff.priceCents) {
        return {
          ...state,
          notice: `Na bilet „${tariff.name}” brakuje ${formatMoney(tariff.priceCents - state.balanceCents)}.`,
        }
      }
      const ticket: PurchasedTicket = {
        id: `t-${tariff.id}-${action.at}`,
        ticketTypeId: tariff.id,
        code: ticketCode(action.at, tariff.id),
        pricePaidCents: tariff.priceCents,
        purchasedAt: action.at,
        validatedAt: null,
        expiresAt: null,
        routeId: null,
      }
      const transaction: WalletTransaction = {
        id: `w-${action.at}`,
        type: "TICKET_PURCHASE",
        amountCents: -tariff.priceCents,
        createdAt: action.at,
        ticketId: ticket.id,
        penaltyId: null,
      }
      return {
        ...state,
        balanceCents: state.balanceCents - tariff.priceCents,
        tickets: [ticket, ...state.tickets],
        transactions: [transaction, ...state.transactions],
        notice: `Kupiono ${tariff.name} za ${formatMoney(tariff.priceCents)}. Kod ${ticket.code} czeka na skasowanie.`,
      }
    }
    case "validate": {
      const ticket = state.tickets.find((item) => item.id === action.ticketId)
      const tariff = ticket
        ? ticketTypes.find((item) => item.id === ticket.ticketTypeId)
        : undefined
      if (!ticket || !tariff || ticket.validatedAt) {
        return state
      }
      if (!action.routeId.trim()) {
        return {
          ...state,
          notice: "Wybierz linię, na której kasujesz bilet.",
        }
      }
      const expiresAt = new Date(
        new Date(action.at).getTime() + tariff.validityMinutes * 60_000
      ).toISOString()
      return {
        ...state,
        tickets: state.tickets.map((item) =>
          item.id === ticket.id
            ? {
                ...item,
                validatedAt: action.at,
                expiresAt,
                routeId: action.routeId,
              }
            : item
        ),
        notice: `${ticket.code} skasowany na linii ${action.routeId}. Ważny do ${new Date(expiresAt).toLocaleTimeString("pl-PL", { hour: "2-digit", minute: "2-digit" })}.`,
      }
    }
    case "pay": {
      const penalty = state.penalties.find(
        (item) => item.id === action.penaltyId
      )
      if (!penalty || penalty.status !== "ISSUED") {
        return state
      }
      if (state.balanceCents < penalty.amountCents) {
        return {
          ...state,
          notice: `Mandat ${penalty.penaltyNumber} wynosi ${formatMoney(penalty.amountCents)}. Na koncie jest ${formatMoney(state.balanceCents)}.`,
        }
      }
      const transaction: WalletTransaction = {
        id: `w-${action.at}`,
        type: "PENALTY_PAYMENT",
        amountCents: -penalty.amountCents,
        createdAt: action.at,
        ticketId: null,
        penaltyId: penalty.id,
      }
      return {
        ...state,
        balanceCents: state.balanceCents - penalty.amountCents,
        penalties: state.penalties.map((item) =>
          item.id === penalty.id ? { ...item, status: "PAID" } : item
        ),
        transactions: [transaction, ...state.transactions],
        notice: `Opłacono mandat ${penalty.penaltyNumber}.`,
      }
    }
    case "toggle-favorite": {
      const exists = state.favoriteStopIds.includes(action.stopId)
      return {
        ...state,
        favoriteStopIds: exists
          ? state.favoriteStopIds.filter((id) => id !== action.stopId)
          : [...state.favoriteStopIds, action.stopId],
        notice: "",
      }
    }
    case "search": {
      const query = action.query.trim()
      if (!query) {
        return state
      }
      const searches = [
        { id: `s-${action.at}`, query, searchedAt: action.at },
        ...state.searches.filter(
          (entry) =>
            entry.query.toLocaleLowerCase("pl") !==
            query.toLocaleLowerCase("pl")
        ),
      ].slice(0, 8)
      return { ...state, searches, notice: "" }
    }
    default:
      return state
  }
}

type AuthResult = { ok: true } | { ok: false; error: string }

type AccountApi = State & {
  signIn: (email: string, password: string) => AuthResult
  register: (email: string, password: string) => AuthResult
  topUp: (amountCents: number) => void
  buy: (ticketTypeId: string) => void
  validate: (ticketId: string, routeId: string) => void
  pay: (penaltyId: string) => void
  toggleFavorite: (stopId: string) => void
  search: (query: string) => void
  clearNotice: () => void
}

const AccountContext = createContext<AccountApi | null>(null)

export function AccountProvider({ children }: { children: ReactNode }) {
  const [store, dispatch] = useReducer(storeReducer, undefined, initialStore)
  const state = store.accounts[store.activeEmail]?.state ?? seed(demoEmail)

  const api = useMemo<AccountApi>(
    () => ({
      ...state,
      signIn: (rawEmail, password) => {
        const email = rawEmail.trim().toLowerCase()
        const existing = store.accounts[email]
        if (existing && existing.password !== null && existing.password !== password) {
          return { ok: false, error: "Nieprawidłowy adres e-mail lub hasło." }
        }
        dispatch({ type: "sign-in", email })
        return { ok: true }
      },
      register: (rawEmail, password) => {
        const email = rawEmail.trim().toLowerCase()
        if (store.accounts[email]) {
          return { ok: false, error: "Konto z tym adresem e-mail już istnieje." }
        }
        dispatch({ type: "register", email, password })
        return { ok: true }
      },
      topUp: (amountCents) =>
        dispatch({ type: "topup", amountCents, at: new Date().toISOString() }),
      buy: (ticketTypeId) =>
        dispatch({ type: "buy", ticketTypeId, at: new Date().toISOString() }),
      validate: (ticketId, routeId) =>
        dispatch({
          type: "validate",
          ticketId,
          routeId,
          at: new Date().toISOString(),
        }),
      pay: (penaltyId) =>
        dispatch({ type: "pay", penaltyId, at: new Date().toISOString() }),
      toggleFavorite: (stopId) => dispatch({ type: "toggle-favorite", stopId }),
      search: (query) =>
        dispatch({ type: "search", query, at: new Date().toISOString() }),
      clearNotice: () => dispatch({ type: "clear-notice" }),
    }),
    [state, store.accounts]
  )

  return (
    <AccountContext.Provider value={api}>{children}</AccountContext.Provider>
  )
}

export function useAccount() {
  const account = useContext(AccountContext)
  if (!account) {
    throw new Error("useAccount must be used within AccountProvider")
  }
  return account
}

export function ticketTypeById(id: string) {
  return ticketTypes.find((item) => item.id === id)
}

export function transactionLabel(type: TransactionType) {
  switch (type) {
    case "TOP_UP":
      return "Doładowanie"
    case "TICKET_PURCHASE":
      return "Zakup biletu"
    case "PENALTY_PAYMENT":
      return "Opłata mandatu"
  }
}

export function penaltyStatusLabel(status: PenaltyStatus) {
  switch (status) {
    case "ISSUED":
      return "Wystawiony"
    case "PAID":
      return "Opłacony"
    case "CANCELLED":
      return "Anulowany"
  }
}
