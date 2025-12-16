import React, { useMemo } from 'react'

interface DurationDisplayProps {
  start: string
  end: string
  compact?: boolean
}

const DurationDisplay: React.FC<DurationDisplayProps> = ({
  start,
  end,
  compact = false,
}) => {
  const duration = useMemo(() => {
    const startDate = new Date(start)
    const endDate = new Date(end)
    const diffMs = endDate.getTime() - startDate.getTime()
    const diffMins = Math.floor(diffMs / 60000)

    if (diffMins < 60) {
      return compact ? `${diffMins}m` : `${diffMins} min`
    }

    const hours = Math.floor(diffMins / 60)
    const minutes = diffMins % 60

    if (compact) {
      return minutes > 0 ? `${hours}h${minutes}m` : `${hours}h`
    }

    return minutes > 0 ? `${hours}h${minutes}min` : `${hours}h`
  }, [start, end, compact])

  return (
    <div className={`duration-display ${compact ? 'compact' : ''}`}>
      <i className="far fa-hourglass"></i>
      {duration}
    </div>
  )
}

export default DurationDisplay
