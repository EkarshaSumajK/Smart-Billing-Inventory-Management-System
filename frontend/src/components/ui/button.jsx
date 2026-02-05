import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva } from "class-variance-authority";

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-semibold tracking-normal transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-apple-blue/30 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-apple-blue text-white shadow-apple-sm hover:bg-apple-blue-light",
        destructive:
          "bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90",
        outline:
          "border border-apple-gray-300 bg-white text-apple-gray-900 shadow-apple-sm hover:bg-apple-gray-50 dark:border-apple-gray-700 dark:bg-apple-gray-900/70 dark:text-apple-gray-100 dark:hover:bg-apple-gray-800",
        secondary:
          "bg-apple-gray-100 text-apple-gray-900 shadow-apple-sm hover:bg-apple-gray-200 dark:bg-apple-gray-800 dark:text-apple-gray-100 dark:hover:bg-apple-gray-700",
        ghost: "hover:bg-apple-gray-100 hover:text-apple-gray-900 dark:hover:bg-apple-gray-800",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-9 px-4 py-2",
        sm: "h-8 px-3 text-xs",
        lg: "h-10 px-6 text-sm",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

const Button = React.forwardRef(({ className, variant, size, asChild = false, ...props }, ref) => {
  const Comp = asChild ? Slot : "button"
  return (
    <Comp
      className={cn(buttonVariants({ variant, size, className }))}
      ref={ref}
      {...props} />
  );
})
Button.displayName = "Button"

export { Button, buttonVariants }
