const money = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
})

const time = new Intl.DateTimeFormat("pl-PL", {
  hour: "2-digit",
  minute: "2-digit",
})

const dateTime = new Intl.DateTimeFormat("pl-PL", {
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
})

const weekday = new Intl.DateTimeFormat("pl-PL", {
  weekday: "long",
  day: "numeric",
  month: "long",
})

export function formatMoney(cents: number) {
  return money.format(cents / 100)
}

export function formatTime(value: Date | string) {
  const date = typeof value === "string" ? new Date(value) : value
  return time.format(date)
}

export function formatDateTime(value: Date | string) {
  const date = typeof value === "string" ? new Date(value) : value
  return dateTime.format(date)
}

export function formatWeekday(value: Date) {
  return weekday.format(value)
}

export function formatRemaining(ms: number) {
  if (ms <= 0) {
    return "0 min"
  }

  const totalMinutes = Math.ceil(ms / 60_000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (hours <= 0) {
    return `${minutes} min`
  }

  return `${hours} godz. ${minutes} min`
}

export function formatValidity(minutes: number) {
  if (minutes % 1440 === 0) {
    return minutes === 1440 ? "24 godziny" : `${minutes / 1440} dni`
  }

  if (minutes >= 60 && minutes % 60 === 0) {
    const hours = minutes / 60
    return hours === 1 ? "1 godzina" : `${hours} godziny`
  }

  return `${minutes} min`
}

export function vehicleLabel(vehicleType: "TRAM" | "BUS") {
  return vehicleType === "TRAM" ? "Tramwaj" : "Autobus"
}
