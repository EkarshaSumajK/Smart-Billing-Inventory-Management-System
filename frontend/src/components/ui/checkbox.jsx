import * as React from "react"
import * as CheckboxPrimitive from "@radix-ui/react-checkbox"
import { Check } from "lucide-react"

import { cn } from "@/lib/utils"

const Checkbox = React.forwardRef(({ className, ...props }, ref) => (
  <CheckboxPrimitive.Root
    ref={ref}
    className={cn(
      "grid place-content-center peer h-4 w-4 shrink-0 rounded-[4px] border border-apple-gray-400 bg-white shadow-apple-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-apple-blue/30 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-apple-blue data-[state=checked]:text-white dark:border-apple-gray-700 dark:bg-apple-gray-900",
      className
    )}
    {...props}>
    <CheckboxPrimitive.Indicator className={cn("grid place-content-center text-current")}>
      <Check className="h-4 w-4" />
    </CheckboxPrimitive.Indicator>
  </CheckboxPrimitive.Root>
))
Checkbox.displayName = CheckboxPrimitive.Root.displayName

export { Checkbox }
