from crewai import Task
from agents import devops_expert

def create_tasks(query, context):
    # Tek bir kapsamlı ama kısa görev
    analysis_task = Task(
        description=f"""
        Analiz Et ve Yanıtla:
        - Kullanıcı İsteği: {query}
        - Sistem Bağlamı: {context}
        
        Sadece bu isteğe odaklan, eğer 'merhaba' diyorsa merhaba de ve yardıma hazır olduğunu belirt. 
        Asla 'Within System', 'Bağlamı' gibi kelimeleri gereksiz yere tekrar etme. 
        Cevabını doğrudan, kısa ve Türkçe olarak ver.
        """,
        expected_output="Kısa, öz ve Türkçe bir DevOps yanıtı.",
        agent=devops_expert
    )
    
    return [analysis_task]
