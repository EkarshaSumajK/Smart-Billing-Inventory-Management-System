import * as React from "react"
import { cva } from "class-variance-authority";

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-medium tracking-normal transition-colors focus:outline-none focus:ring-2 focus:ring-apple-blue/30 focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-apple-blue/20 bg-apple-blue/10 text-apple-blue hover:bg-apple-blue/20",
        secondary:
          "border-transparent bg-apple-gray-100 text-apple-gray-700 hover:bg-apple-gray-200 dark:bg-apple-gray-800 dark:text-apple-gray-100 dark:hover:bg-apple-gray-700",
        destructive:
          "border-destructive/20 bg-destructive/10 text-destructive hover:bg-destructive/20",
        outline: "text-apple-gray-700 border-apple-gray-300 dark:text-apple-gray-100 dark:border-apple-gray-700",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function Badge({
  className,
  variant,
  ...props
}) {
  return (<div className={cn(badgeVariants({ variant }), className)} {...props} />);
}

export { Badge, badgeVariants }
