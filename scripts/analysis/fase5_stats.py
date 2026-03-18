import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from scipy import stats

# Configuración de rutas
DATA_DIR = "ai/data/datasets"
OUTPUT_DIR = "data/reports/phase5"
os.makedirs(OUTPUT_DIR, exist_ok=True)


def analyze_win_rates(df, matchup_label):
    print(f"\n--- Análisis de Win Rate: {matchup_label} ---")
    total = len(df)
    # Normalizamos a mayúsculas para evitar problemas de case (p1 vs P1)
    df["winner_id"] = df["winner_id"].astype(str).str.upper()

    p1_wins = len(df[df["winner_id"] == "P1"])
    p2_wins = len(df[df["winner_id"] == "P2"])
    draws = len(df[df["winner_id"] == "DRAW"])

    p1_rate = p1_wins / total
    p2_rate = p2_wins / total

    # En caso de que no haya una columna 'winner_id', el total sería 0 o NaN
    if total == 0:
        print("Error: DataFrame vacío")
        return None, None
    z = 1.96
    ci_p1 = z * np.sqrt((p1_rate * (1 - p1_rate)) / total)
    ci_p2 = z * np.sqrt((p2_rate * (1 - p2_rate)) / total)

    print(f"P1 Win Rate: {p1_rate:.2%} ± {ci_p1:.2%}")
    print(f"P2 Win Rate: {p2_rate:.2%} ± {ci_p2:.2%}")
    print(f"Empates: {draws} ({draws / total:.2%})")

    # Test de Hipótesis (Chi-cuadrado para independencia)
    # H0: La dificultad no afecta al resultado (distribución 50/50 ignorando empates)
    observed = [p1_wins, p2_wins]
    chi2, p_val = stats.chisquare(observed)
    print(f"P-value (Chi-cuadrado): {p_val:.4f}")
    if p_val < 0.05:
        print("Resultado: Significancia estadística detectada (H0 rechazada).")
    else:
        print("Resultado: No hay diferencia significativa (H0 no rechazada).")


def plot_distributions(df, matchup_label):
    plt.figure(figsize=(12, 6))

    # Distribución de Turnos
    plt.subplot(1, 2, 1)
    sns.histplot(df["total_turns"], kde=True, color="blue")
    plt.title(f"Distribución de Turnos - {matchup_label}")
    plt.xlabel("Turnos")

    # Distribución de Patrimonio (Net Worth)
    plt.subplot(1, 2, 2)
    p1_label = df["p1_difficulty"].iloc[0]
    p2_label = df["p2_difficulty"].iloc[0]
    sns.kdeplot(df["p1_final_net_worth"], label=f"NW {p1_label}", fill=True)
    sns.kdeplot(df["p2_final_net_worth"], label=f"NW {p2_label}", fill=True)
    plt.title("Distribución de Patrimonio Final")
    plt.legend()

    plt.tight_layout()
    plt.savefig(f"{OUTPUT_DIR}/{matchup_label.replace(' ', '_').lower()}_analysis.png")
    plt.close()


def main():
    files = ["easy_vs_medium.csv", "medium_vs_hard.csv", "easy_vs_hard.csv"]

    summary_stats = []

    for file in files:
        path = f"{DATA_DIR}/{file}"
        if not os.path.exists(path):
            print(f"Advertencia: No se encuentra {path}")
            continue

        df = pd.read_csv(path)
        matchup = f"{df['p1_difficulty'].iloc[0]} vs {df['p2_difficulty'].iloc[0]}"

        analyze_win_rates(df, matchup)
        plot_distributions(df, matchup)

        # Medias para tabla LaTeX
        summary_stats.append(
            {
                "Matchup": matchup,
                "Avg Turns": df["total_turns"].mean(),
                "P1 NetWorth": df["p1_final_net_worth"].mean(),
                "P2 NetWorth": df["p2_final_net_worth"].mean(),
                "P1 Latency (ms)": df["game_duration_ms"].mean()
                / df["total_turns"].mean(),  # Aproximado
            }
        )

    # Generar tabla resumen en formato LaTeX
    summary_df = pd.DataFrame(summary_stats)
    with open(f"{OUTPUT_DIR}/summary_table.tex", "w") as f:
        f.write(summary_df.to_latex(index=False))

    print(f"\nReportes generados en: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
