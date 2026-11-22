import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"
import { Slot } from "radix-ui"

const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-1.5 rounded-md border border-transparent text-sm font-medium whitespace-nowrap transition-[background-color,border-color,color] duration-100 outline-none select-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background active:translate-y-px disabled:pointer-events-none disabled:opacity-45 aria-invalid:border-destructive [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground hover:bg-[color-mix(in_oklch,var(--primary),var(--background)_18%)]",
        outline:
          "border-input bg-card text-foreground hover:border-foreground hover:bg-muted",
        secondary:
          "bg-secondary text-secondary-foreground hover:bg-[color-mix(in_oklch,var(--secondary),var(--foreground)_7%)]",
        ghost: "text-foreground hover:bg-muted",
        destructive:
          "bg-destructive text-primary-foreground hover:bg-[color-mix(in_oklch,var(--destructive),black_14%)] focus-visible:ring-destructive",
        link: "h-auto px-0 text-foreground underline decoration-border underline-offset-4 hover:decoration-foreground",
      },
      size: {
        default: "h-9 px-3",
        sm: "h-8 px-2.5 text-[0.8125rem]",
        lg: "h-10 px-4",
        icon: "size-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
