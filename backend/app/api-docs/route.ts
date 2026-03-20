import { ApiReference } from "@scalar/nextjs-api-reference";
import { openapi } from "@/lib/openapi";

export const GET = ApiReference({
  theme: "kepler",
  layout: "modern",
  showSidebar: true,
  darkMode: true,
  title: "Muses API 文档",
  content: openapi,
});
