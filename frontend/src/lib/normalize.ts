export function normalizeQuery(value: string) {
  return value.toLocaleLowerCase("pl").normalize("NFD").replace(/\p{M}/gu, "")
}
