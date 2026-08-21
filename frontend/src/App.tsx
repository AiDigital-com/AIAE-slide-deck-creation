import { AppShell } from "./app/AppShell";
import { StrategyToolkit } from "./features/strategy-toolkit/ui/StrategyToolkit";
import "./App.css";

export default function App() {
    return (
        <AppShell appName="Strategy & Planning">
            <StrategyToolkit />
        </AppShell>
    );
}
