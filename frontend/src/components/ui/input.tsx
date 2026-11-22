import * as React from "react"
import { cn } from "cn"

const fieldClassName =
  "h-10 w-full min-w-0 rounded-md border border-input bg-card px-3 text-base text-foreground transition-colors outline-none placeholder:text-muted-foreground/80 hover:border-foreground/60 focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/35 disabled:cursor-not-allowed disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-2 aria-invalid:ring-destructive/25 md:text-sm"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(fieldClassName, className)}
      {...props}
    />
  )
}

function NativeSelect({ className, ...props }: React.ComponentProps<"select">) {
  return (
    <select
      data-slot="select"
      className={cn(fieldClassName, "appearance-auto pr-2", className)}
      {...props}
    />
  )
}

export { Input, NativeSelect }
