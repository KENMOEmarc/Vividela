const DashboardFooter = () => (
  <footer className="h-9 bg-white border-t border-slate-100 flex items-center justify-between px-5 flex-shrink-0">
    <p className="text-[10px] text-slate-400">
      © {new Date().getFullYear()} Vividela
    </p>
    <p className="text-[10px] text-slate-300 hidden sm:block">v1.0.0</p>
  </footer>
)

export default DashboardFooter
