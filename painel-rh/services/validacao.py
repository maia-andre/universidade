"""Validações de entrada do painel e geração de senha temporária."""
import re
import secrets

_RE_EMAIL = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# Sem caracteres ambíguos (0/O, 1/l/I) — a senha é ditada/anotada pelo RH.
_ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"

COLUNAS_PLANILHA = ("nome", "email", "matricula", "lotacao")


def email_valido(email: str) -> bool:
    return bool(_RE_EMAIL.match((email or "").strip()))


def gerar_senha_temporaria(tamanho: int = 8) -> str:
    """Senha aleatória por aluno/operador — substitui a senha padrão fixa."""
    return "".join(secrets.choice(_ALFABETO) for _ in range(tamanho))


def validar_planilha(df, existentes: list[dict]) -> tuple[list[dict], list[dict]]:
    """Dry-run da importação de alunos: valida SEM criar nada.

    `existentes` = alunos já cadastrados (para checar duplicidade de email/matrícula).
    Retorna `(validas, problemas)`: linhas prontas para importar e problemas por linha
    (`{"linha": n, "email": ..., "problema": ...}`). Levanta ValueError se faltar
    coluna obrigatória.
    """
    df = df.rename(columns={c: str(c).strip().lower() for c in df.columns})
    faltantes = [c for c in COLUNAS_PLANILHA if c not in df.columns]
    if faltantes:
        raise ValueError(
            "Planilha sem as colunas obrigatórias: " + ", ".join(faltantes)
            + ". Use o modelo `modelo_importacao.csv` (colunas: nome, email, matricula, lotacao)."
        )

    emails_cadastrados = {str(a.get("email", "")).strip().lower() for a in existentes}
    matriculas_cadastradas = {
        str(a.get("matricula", "")).strip() for a in existentes if a.get("matricula")
    }

    validas, problemas = [], []
    emails_na_planilha: set[str] = set()
    for i, row in df.iterrows():
        linha = i + 2  # 1-based + cabeçalho, como o operador vê no Excel
        nome = str(row.get("nome") or "").strip()
        email = str(row.get("email") or "").strip()
        matricula = str(row.get("matricula") or "").strip()
        lotacao = str(row.get("lotacao") or "").strip()

        problema = None
        if not nome:
            problema = "nome vazio"
        elif not email_valido(email):
            problema = f"e-mail inválido: '{email}'"
        elif email.lower() in emails_na_planilha:
            problema = "e-mail repetido na própria planilha"
        elif email.lower() in emails_cadastrados:
            problema = "e-mail já cadastrado"
        elif matricula and matricula in matriculas_cadastradas:
            problema = f"matrícula '{matricula}' já cadastrada"

        if problema:
            problemas.append({"linha": linha, "email": email or "(vazio)", "problema": problema})
        else:
            emails_na_planilha.add(email.lower())
            validas.append(
                {"nome": nome, "email": email, "matricula": matricula, "lotacao": lotacao}
            )
    return validas, problemas
